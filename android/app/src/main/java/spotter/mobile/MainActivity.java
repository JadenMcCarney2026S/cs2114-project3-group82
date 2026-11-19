package spotter.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity
{
    private static final int PAPER = Color.rgb(245, 244, 237);
    private static final int INK = Color.rgb(22, 35, 28);
    private static final int MUTED = Color.rgb(73, 94, 82);
    private static final int LIME = Color.rgb(212, 247, 112);
    private static final int WHITE = Color.WHITE;
    private static final int PHOTO_PICKER = 17;
    private static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    private static final int PROFILE = 1;
    private static final int DISCOVER = 2;
    private static final int MESSAGES = 3;
    private static final String SERVER = "https://127.0.0.1:8443/chat";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<Integer, SpotterApi.Profile> profiles = new HashMap<>();
    private final Map<Integer, Draft> drafts = new HashMap<>();
    private final ArrayDeque<String> history = new ArrayDeque<>();
    private final Runnable poll = new Runnable()
    {
        @Override
        public void run()
        {
            if (screen >= DISCOVER && !busy)
            {
                refresh();
            }
            main.postDelayed(this, 1500);
        }
    };

    private SpotterApi api;
    private LinearLayout column;
    private TextView status;
    private LinearLayout candidatePanel;
    private int shownCandidate;
    private Spinner matches;
    private TextView transcript;
    private ScrollView transcriptScroll;
    private EditText composer;
    private ImageView photoPreview;
    private TextView photoStatus;
    private byte[] photo;
    private List<Integer> matchIds = new ArrayList<>();
    private int screen;
    private int candidateId;
    private int peer;
    private int cursor;
    private boolean busy;
    private boolean polling;
    private boolean paused;
    private boolean selectingMatch;
    private Runnable queued;

    private record Draft(String text, String id)
    {
    }

    private record Discovery(SpotterApi.Snapshot snapshot, Map<Integer, SpotterApi.Profile> profiles)
    {
    }

    private record Photo(byte[] bytes, Bitmap preview)
    {
    }

    @FunctionalInterface
    private interface Task<T>
    {
        T run() throws Exception;
    }

    @FunctionalInterface
    private interface Result<T>
    {
        void show(T value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        showLogin();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        paused = false;
        main.removeCallbacks(poll);
        main.post(poll);
    }

    @Override
    protected void onPause()
    {
        paused = true;
        main.removeCallbacks(poll);
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
        super.onDestroy();
    }

    private void showLogin()
    {
        screen = 0;
        api = null;
        photo = null;
        profiles.clear();
        drafts.clear();
        matchIds = new ArrayList<>();
        candidateId = 0;
        peer = 0;
        cursor = 0;
        history.clear();
        queued = null;
        page("Find your\ntraining partner.");
        EditText username = field("Username", InputType.TYPE_CLASS_TEXT);
        EditText password = field("Password", InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        username.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
        password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
        add(button("Log in", true, () -> authenticate(username, password, false)));
        add(button("Sign up", false, () -> authenticate(username, password, true)));
    }

    private void authenticate(EditText username, EditText password, boolean newAccount)
    {
        String name = value(username).trim();
        String secret = value(password);
        if (name.isEmpty() || secret.isEmpty())
        {
            alert("Check your details", "Enter your username and password.");
            return;
        }
        if (newAccount && (!name.matches("[A-Za-z0-9_]{3,32}")
            || secret.length() < 12 || secret.length() > 128))
        {
            alert("Check your details", "Use 3–32 letters, digits, or underscores for your username "
                + "and 12–128 characters for your password.");
            return;
        }
        run(false, () -> {
            SpotterApi client;
            try (InputStream certificate = getAssets().open("spotter-server.crt"))
            {
                client = new SpotterApi(SERVER, certificate);
            }
            catch (java.io.FileNotFoundException missingCertificate)
            {
                throw new IOException("Set up the emulator connection and rebuild the app. "
                    + "The Spotter server certificate is missing.", missingCertificate);
            }
            boolean complete = newAccount ? client.signUp(name, secret) : client.login(name, secret);
            return new Object[] { client, complete };
        }, result -> {
            api = (SpotterApi)result[0];
            if ((Boolean)result[1])
            {
                showDiscovery();
            }
            else
            {
                showProfile();
            }
        });
    }

    private void showProfile()
    {
        screen = PROFILE;
        page("Create your profile");
        EditText name = field("Display name", InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText age = field("Age (18+)", InputType.TYPE_CLASS_NUMBER);
        EditText interest = field("Workout interest", InputType.TYPE_CLASS_TEXT);
        EditText bio = field("Bio (optional)", InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        photoPreview = new ImageView(this);
        photoPreview.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(170)));
        photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoPreview.setContentDescription("Selected profile photo");
        photoPreview.setVisibility(View.GONE);
        add(photoPreview);
        photoStatus = text("JPEG or PNG, up to 2 MB", 14, MUTED);
        add(photoStatus);
        add(button("Choose a photo", false, this::choosePhoto));
        add(button("Save profile", true, () -> saveProfile(name, age, interest, bio)));
        add(button("Log out", false, this::logout));
    }

    private void choosePhoto()
    {
        Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "image/jpeg", "image/png" });
        startActivityForResult(open, PHOTO_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PHOTO_PICKER || resultCode != RESULT_OK || data == null
            || data.getData() == null || screen != PROFILE)
        {
            return;
        }
        Uri selected = data.getData();
        run(false, () -> readPhoto(selected), image -> {
            photo = image.bytes();
            photoPreview.setImageBitmap(image.preview());
            photoPreview.setVisibility(View.VISIBLE);
            photoStatus.setText("Photo ready");
        });
    }

    private Photo readPhoto(Uri selected) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = getContentResolver().openInputStream(selected))
        {
            if (input == null)
            {
                throw new IOException("The photo could not be opened.");
            }
            byte[] buffer = new byte[8192];
            for (int count; (count = input.read(buffer)) != -1;)
            {
                if (output.size() + count > MAX_PHOTO_BYTES)
                {
                    throw new IOException("Choose a JPEG or PNG no larger than 2 MB.");
                }
                output.write(buffer, 0, count);
            }
        }
        byte[] bytes = output.toByteArray();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (!"image/jpeg".equals(bounds.outMimeType) && !"image/png".equals(bounds.outMimeType)
            || bounds.outWidth <= 0 || bounds.outHeight <= 0
            || bounds.outWidth > 4096 || bounds.outHeight > 4096)
        {
            throw new IOException("Choose a JPEG or PNG no larger than 4096 × 4096 pixels.");
        }
        BitmapFactory.Options scaled = new BitmapFactory.Options();
        scaled.inSampleSize = Math.max(1, Integer.highestOneBit(
            Math.max(bounds.outWidth, bounds.outHeight) / 512));
        Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, scaled);
        if (image == null)
        {
            throw new IOException("That photo could not be read. Choose a different one.");
        }
        return new Photo(bytes, image);
    }

    private void saveProfile(EditText name, EditText age, EditText interest, EditText bio)
    {
        int years;
        try
        {
            years = Integer.parseInt(value(age).trim());
        }
        catch (NumberFormatException invalidAge)
        {
            alert("Check your age", "Enter your age as a whole number.");
            return;
        }
        String displayName = value(name).trim();
        String activity = value(interest).trim();
        String about = value(bio).trim();
        if (years < 18 || displayName.isEmpty() || activity.isEmpty() || photo == null)
        {
            alert("Finish your profile", "Add a display name, an age of 18 or older, "
                + "a workout interest, and a photo.");
            return;
        }
        byte[] chosen = photo;
        run(false, () -> {
            api.complete(displayName, years, activity, about, chosen);
            return true;
        }, ignored -> {
            photo = null;
            showDiscovery();
        });
    }

    private void showDiscovery()
    {
        keepDraft();
        screen = DISCOVER;
        page("Discover");
        navigation();
        candidatePanel = new LinearLayout(this);
        candidatePanel.setOrientation(LinearLayout.VERTICAL);
        add(candidatePanel);
        shownCandidate = -1;
        showCandidate();
        refresh();
    }

    private void showCandidate()
    {
        if (screen != DISCOVER || shownCandidate == candidateId)
        {
            return;
        }
        shownCandidate = candidateId;
        candidatePanel.removeAllViews();
        SpotterApi.Profile candidate = profiles.get(candidateId);
        if (candidate == null)
        {
            candidatePanel.addView(text("No profiles to show yet.", 18, MUTED));
            return;
        }
        byte[] bytes = candidate.photo();
        Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ImageView portrait = new ImageView(this);
        portrait.setImageBitmap(image);
        portrait.setContentDescription(candidate.name() + " profile photo");
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        portrait.setBackground(card(WHITE, 24));
        portrait.setClipToOutline(true);
        candidatePanel.addView(portrait, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(290)));
        TextView name = text(candidate.name() + ", " + candidate.age(), 28, INK);
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(15), 0, dp(4));
        candidatePanel.addView(name);
        candidatePanel.addView(text(candidate.interest(), 16, MUTED));
        if (!candidate.bio().isEmpty())
        {
            TextView bio = text(candidate.bio(), 16, INK);
            bio.setPadding(0, dp(8), 0, dp(12));
            candidatePanel.addView(bio);
        }
        int current = candidateId;
        LinearLayout actions = new LinearLayout(this);
        actions.addView(button("Pass", false, () -> swipe("left", current)),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(button("Like", true, () -> swipe("right", current)),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        candidatePanel.addView(actions);
    }

    private void swipe(String action, int target)
    {
        run(false, () -> discovery(action, target, "", "", 0), result -> {
            applyDiscovery(result, -1);
            if (!result.snapshot().accepted())
            {
                alert("Profile changed", "Check the current profile before choosing Like or Pass.");
            }
        });
    }

    private void showMessages()
    {
        screen = MESSAGES;
        page("Messages");
        navigation();
        matches = new Spinner(this);
        matches.setContentDescription("Choose a mutual match");
        add(matches);
        transcript = text("", 16, INK);
        transcript.setTextIsSelectable(true);
        transcript.setPadding(dp(16), dp(12), dp(16), dp(12));
        transcriptScroll = new ScrollView(this);
        transcriptScroll.setBackground(card(WHITE, 18));
        transcriptScroll.addView(transcript);
        transcriptScroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(230)));
        add(transcriptScroll);
        composer = field("Message", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        add(button("Send", true, this::send));
        updateMatches();
        updateTranscript();
        refresh();
    }

    private void navigation()
    {
        LinearLayout tabs = new LinearLayout(this);
        tabs.addView(button("Discover", screen == DISCOVER, () -> {
            if (screen != DISCOVER)
            {
                showDiscovery();
            }
        }));
        tabs.addView(button("Messages", screen == MESSAGES, () -> {
            if (screen != MESSAGES)
            {
                showMessages();
            }
        }));
        tabs.addView(button("Log out", false, this::logout));
        add(tabs);
    }

    private void updateMatches()
    {
        if (screen != MESSAGES)
        {
            return;
        }
        keepDraft();
        selectingMatch = true;
        List<String> names = new ArrayList<>();
        for (int id : matchIds)
        {
            SpotterApi.Profile profile = profiles.get(id);
            names.add(profile == null ? "Match " + id : profile.name());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, names);
        matches.setAdapter(adapter);
        int selected = matchIds.indexOf(peer);
        if (selected < 0)
        {
            setPeer(matchIds.isEmpty() ? 0 : matchIds.get(0));
            selected = matchIds.isEmpty() ? -1 : 0;
        }
        if (selected >= 0)
        {
            matches.setSelection(selected);
        }
        composer.setText(draftText(peer));
        composer.setEnabled(peer != 0);
        selectingMatch = false;
        matches.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (!selectingMatch && position < matchIds.size()
                    && matchIds.get(position) != peer)
                {
                    keepDraft();
                    setPeer(matchIds.get(position));
                    composer.setText(draftText(peer));
                    updateTranscript();
                    refresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    private void setPeer(int target)
    {
        if (peer != target)
        {
            peer = target;
            cursor = 0;
            history.clear();
        }
    }

    private void updateTranscript()
    {
        if (screen != MESSAGES)
        {
            return;
        }
        transcript.setText(peer == 0 ? "Mutual likes open a chat here."
            : history.isEmpty() ? "Say hello to your training partner."
            : String.join("\n\n", history));
        transcriptScroll.post(() -> transcriptScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void refresh()
    {
        if (api == null || screen < DISCOVER)
        {
            return;
        }
        int target = screen == MESSAGES ? peer : 0;
        int after = target == 0 ? 0 : cursor;
        if (busy)
        {
            return;
        }
        run(true, () -> discovery("view", target, "", "", after),
            result -> applyDiscovery(result, target));
    }

    private Discovery discovery(String action, int target, String message, String sendId, int after)
        throws Exception
    {
        SpotterApi.Snapshot snapshot = api.request(action, target, message, sendId, after);
        Map<Integer, SpotterApi.Profile> loaded = new HashMap<>();
        if (snapshot.candidate() > 0 && !profiles.containsKey(snapshot.candidate()))
        {
            loaded.put(snapshot.candidate(), api.profile(snapshot.candidate()));
        }
        for (int match : snapshot.matches())
        {
            if (!profiles.containsKey(match) && !loaded.containsKey(match))
            {
                loaded.put(match, api.profile(match));
            }
        }
        return new Discovery(snapshot, loaded);
    }

    private void applyDiscovery(Discovery result, int target)
    {
        SpotterApi.Snapshot snapshot = result.snapshot();
        profiles.putAll(result.profiles());
        candidateId = snapshot.candidate();
        showCandidate();
        if (!matchIds.equals(snapshot.matches()))
        {
            matchIds = new ArrayList<>(snapshot.matches());
            updateMatches();
        }
        if (peer == target && target != 0)
        {
            if (cursor != snapshot.start())
            {
                history.clear();
            }
            if (!snapshot.history().isEmpty())
            {
                for (String line : snapshot.history().split("\n", -1))
                {
                    history.addLast(line);
                    if (history.size() > 100)
                    {
                        history.removeFirst();
                    }
                }
            }
            cursor = snapshot.next();
            updateTranscript();
        }
    }

    private void send()
    {
        int target = peer;
        if (target == 0)
        {
            alert("Choose a match", "Messages are available after a mutual like.");
            return;
        }
        String message = value(composer);
        if (message.trim().isEmpty() || message.length() > 500)
        {
            alert("Check your message", "Enter a message between 1 and 500 characters.");
            return;
        }
        keepDraft();
        Draft sending = drafts.get(target);
        int after = cursor;
        run(false, () -> discovery("send", target, message, sending.id(), after), result -> {
            applyDiscovery(result, target);
            if (!result.snapshot().accepted())
            {
                alert("Message not sent", "Check the selected match and your message.");
                return;
            }
            keepDraft();
            if (sending.equals(drafts.get(target)))
            {
                drafts.remove(target);
                if (screen == MESSAGES && peer == target)
                {
                    composer.setText("");
                }
            }
        });
    }

    private String draftText(int target)
    {
        Draft saved = drafts.get(target);
        return saved == null ? "" : saved.text();
    }

    private void keepDraft()
    {
        if (screen != MESSAGES || peer == 0 || composer == null)
        {
            return;
        }
        String message = value(composer);
        if (message.isEmpty())
        {
            drafts.remove(peer);
        }
        else if (!message.equals(draftText(peer)))
        {
            drafts.put(peer, new Draft(message, UUID.randomUUID().toString()));
        }
    }

    private void logout()
    {
        run(false, () -> {
            api.logout();
            return true;
        }, ignored -> showLogin());
    }

    private <T> void run(boolean automatic, Task<T> task, Result<T> done)
    {
        if (busy)
        {
            if (!automatic && polling && queued == null)
            {
                queued = () -> run(false, task, done);
            }
            return;
        }
        busy = true;
        polling = automatic;
        if (!automatic)
        {
            status.setText("Working…");
        }
        worker.execute(() -> {
            try
            {
                T value = task.run();
                main.post(() -> finish(() -> {
                    status.setText("");
                    done.show(value);
                }));
            }
            catch (Exception failure)
            {
                main.post(() -> finish(() -> report(failure, automatic)));
            }
        });
    }

    private void finish(Runnable result)
    {
        if (isFinishing() || isDestroyed())
        {
            return;
        }
        result.run();
        busy = false;
        polling = false;
        Runnable next = queued;
        queued = null;
        if (next != null)
        {
            next.run();
        }
    }

    private void report(Exception failure, boolean automatic)
    {
        String message = failure.getMessage();
        if (failure instanceof SpotterApi.SessionExpired)
        {
            showLogin();
            alert("Session ended", "Log in again to continue.");
        }
        else if (automatic)
        {
            status.setText("Connection interrupted. Trying again…");
        }
        else
        {
            status.setText("");
            alert("Couldn’t complete that action", message == null
                ? "Check the Spotter server and try again." : message);
        }
    }

    private void page(String title)
    {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(22), dp(24), dp(22), dp(24));
        scroll.addView(column);
        setContentView(scroll);
        TextView brand = text("SPOTTER", 16, INK);
        brand.setTypeface(null, Typeface.BOLD_ITALIC);
        brand.setGravity(Gravity.CENTER);
        brand.setBackground(card(LIME, 16));
        brand.setPadding(dp(12), dp(6), dp(12), dp(6));
        column.addView(brand, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView heading = text(title, 34, INK);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, dp(14), 0, dp(10));
        heading.setAccessibilityHeading(true);
        add(heading);
        status = text("", 14, MUTED);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(status);
    }

    private EditText field(String label, int type)
    {
        TextView title = text(label, 14, MUTED);
        EditText field = new EditText(this);
        field.setId(View.generateViewId());
        title.setLabelFor(field.getId());
        add(title);
        field.setTextColor(INK);
        field.setInputType(type);
        field.setTextSize(17);
        field.setBackground(card(WHITE, 12));
        field.setPadding(dp(13), dp(10), dp(13), dp(10));
        field.setMinHeight(dp(48));
        add(field);
        return field;
    }

    private Button button(String label, boolean primary, Runnable action)
    {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(INK);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setBackgroundTintList(ColorStateList.valueOf(primary ? LIME : WHITE));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private TextView text(String value, int size, int color)
    {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void add(View child)
    {
        ViewGroup.LayoutParams existing = child.getLayoutParams();
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, existing == null
                ? ViewGroup.LayoutParams.WRAP_CONTENT : existing.height);
        layout.bottomMargin = dp(8);
        column.addView(child, layout);
    }

    private static String value(EditText field)
    {
        return field.getText().toString();
    }

    private GradientDrawable card(int color, int radius)
    {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private int dp(float value)
    {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void alert(String heading, String detail)
    {
        if (!paused)
        {
            new AlertDialog.Builder(this).setTitle(heading).setMessage(detail)
                .setPositiveButton(android.R.string.ok, null).show();
        }
    }
}
