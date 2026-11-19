package spotter;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Hosts account access and required profile setup before discovery. */
final class SpotterLanOnboarding
{
    private static final int PREVIEW_EDGE = 160;
    private static final int MAX_PHOTO_DIMENSION = 4096;

    record Photo(byte[] bytes, ImageIcon preview)
    {
    }

    private final JFrame window = new JFrame("Spotter");
    private final JTextField address = new JTextField(27);
    private final JTextField username = new JTextField(27);
    private final JPasswordField password = new JPasswordField(27);
    private final JLabel status = new JLabel(" ");
    private final BiConsumer<URI, SpotterLanClient> onComplete;
    private URI serverAddress;
    private Photo photo;

    SpotterLanOnboarding(URI server, String notice, BiConsumer<URI, SpotterLanClient> onComplete)
    {
        this.onComplete = onComplete;
        address.setText(server == null ? "https://127.0.0.1:8080/chat" : server.toString());
        status.putClientProperty("html.disable", true);
        status.setText(notice);
        showLogin();
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setLocationByPlatform(true);
        window.setVisible(true);
    }

    private void showLogin()
    {
        JPanel form = form();
        addField(form, "Server URL", address);
        addField(form, "Username", username);
        addField(form, "Password", password);
        JPanel actions = new JPanel();
        JLabel requirements = new JLabel("<html>Sign-up username: 3–32 letters, digits, or underscores.<br>"
            + "Password: 12–128 characters.</html>");
        form.add(requirements);
        JButton login = new JButton("Log in");
        JButton signUp = new JButton("Sign up");
        actions.add(login);
        actions.add(signUp);
        login.addActionListener(event -> authenticate(false, login, signUp));
        signUp.addActionListener(event -> authenticate(true, login, signUp));
        show(form, actions, login);
    }

    private void authenticate(boolean signUp, JButton login, JButton register)
    {
        SpotterLanClient client;
        try
        {
            serverAddress = URI.create(address.getText().trim());
            client = new SpotterLanClient(serverAddress);
        }
        catch (IllegalArgumentException error)
        {
            status.setText("Enter an HTTPS server URL ending in /chat.");
            return;
        }
        String name = username.getText().trim();
        char[] entry = password.getPassword();
        password.setText("");
        if (name.isEmpty() || entry.length == 0)
        {
            Arrays.fill(entry, '\0');
            status.setText("Enter a username and password.");
            return;
        }
        work("Connecting…", () -> {
            try
            {
                String passwordText = new String(entry);
                return signUp ? client.signUp(name, passwordText) : client.login(name, passwordText);
            }
            finally
            {
                Arrays.fill(entry, '\0');
            }
        }, account -> {
            if (account.profileComplete())
            {
                openDiscovery(client);
            }
            else
            {
                status.setText("Add a photo and tell other members about yourself.");
                showProfile(client);
            }
        }, login, register);
    }

    private void showProfile(SpotterLanClient client)
    {
        JPanel form = form();
        JTextField displayName = new JTextField(27);
        JTextField age = new JTextField(27);
        JTextField interest = new JTextField(27);
        JTextField bio = new JTextField(27);
        addField(form, "Display name", displayName);
        addField(form, "Age (18+)", age);
        addField(form, "Workout interest", interest);
        addField(form, "Bio (optional)", bio);
        JLabel preview = new JLabel("No photo selected");
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel actions = new JPanel();
        JButton choose = new JButton("Choose photo");
        JButton save = new JButton("Save profile");
        JButton cancel = new JButton("Log out");
        actions.add(choose);
        actions.add(save);
        actions.add(cancel);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(form, BorderLayout.NORTH);
        content.add(preview, BorderLayout.CENTER);
        choose.addActionListener(event -> choosePhoto(preview, choose, save, cancel));
        save.addActionListener(event -> saveProfile(client, displayName.getText(), age.getText(),
            interest.getText(), bio.getText(), choose, save, cancel));
        cancel.addActionListener(event -> work("Logging out…", () -> {
            client.logout();
            return null;
        }, ignored -> backToLogin(), choose, save, cancel));
        show(content, actions, save);
    }

    private void choosePhoto(JLabel preview, JButton... buttons)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JPEG and PNG images", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION)
        {
            Path path = chooser.getSelectedFile().toPath();
            work("Loading photo…", () -> readPhoto(path), photo -> {
                this.photo = photo;
                preview.setText("");
                preview.setIcon(photo.preview());
                preview.getAccessibleContext().setAccessibleName("Selected profile photo");
                window.pack();
                status.setText("Photo ready.");
            }, buttons);
        }
    }

    private void saveProfile(SpotterLanClient client, String name, String ageText,
        String interest, String bio, JButton... buttons)
    {
        int age;
        try
        {
            age = Integer.parseInt(ageText.trim());
        }
        catch (NumberFormatException error)
        {
            status.setText("Enter your age as a whole number.");
            return;
        }
        if (age < SpotterAccounts.MIN_AGE)
        {
            status.setText("You must be at least 18 to create a profile.");
            return;
        }
        if (photo == null || name.isBlank() || interest.isBlank())
        {
            status.setText("Enter a display name and workout interest, and choose a photo.");
            return;
        }
        byte[] uploaded = photo.bytes();
        work("Saving profile…", () -> client.completeProfile(name.trim(), age, interest.trim(),
            bio.trim(), uploaded), account -> {
                if (account.profileComplete())
                {
                    openDiscovery(client);
                }
                else
                {
                    status.setText("Profile setup is incomplete.");
                }
            }, buttons);
    }

    private void backToLogin()
    {
        photo = null;
        status.setText("Log in or create an account.");
        showLogin();
    }

    private void openDiscovery(SpotterLanClient client)
    {
        window.dispose();
        onComplete.accept(serverAddress, client);
    }

    private <T> void work(String progress, Callable<T> task, Consumer<T> onSuccess,
        JButton... buttons)
    {
        for (JButton button : buttons)
        {
            button.setEnabled(false);
        }
        status.setText(progress);
        new SwingWorker<T, Void>()
        {
            @Override
            protected T doInBackground() throws Exception
            {
                return task.call();
            }

            @Override
            protected void done()
            {
                if (!window.isDisplayable())
                {
                    return;
                }
                for (JButton button : buttons)
                {
                    button.setEnabled(true);
                }
                try
                {
                    onSuccess.accept(get());
                }
                catch (ExecutionException error)
                {
                    Throwable cause = error.getCause();
                    if (cause instanceof SpotterLanClient.SessionExpiredException)
                    {
                        backToLogin();
                    }
                    else
                    {
                        status.setText(cause.getMessage());
                    }
                }
                catch (InterruptedException error)
                {
                    Thread.currentThread().interrupt();
                    status.setText("Request interrupted.");
                }
            }
        }.execute();
    }

    private void show(JComponent fields, JComponent actions, JButton submit)
    {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        body.add(fields, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(actions, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        window.setContentPane(body);
        window.getRootPane().setDefaultButton(submit);
        window.pack();
    }

    private static JPanel form()
    {
        return new JPanel(new GridLayout(0, 1, 0, 4));
    }

    private static void addField(JPanel form, String name, JComponent field)
    {
        JLabel label = new JLabel(name);
        label.setLabelFor(field);
        form.add(label);
        form.add(field);
    }

    static Photo readPhoto(Path path) throws IOException
    {
        byte[] bytes;
        try (var input = Files.newInputStream(path))
        {
            bytes = input.readNBytes(SpotterAccounts.MAX_PHOTO_BYTES + 1);
        }
        if (bytes.length > SpotterAccounts.MAX_PHOTO_BYTES)
        {
            throw new IOException("Choose a photo no larger than 2 MB.");
        }
        try (var image = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes)))
        {
            var readers = ImageIO.getImageReaders(image);
            if (!readers.hasNext())
            {
                throw new IOException("Choose a valid JPEG or PNG photo.");
            }
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(image, true, true);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("jpeg") && !format.equalsIgnoreCase("png"))
                {
                    throw new IOException("Choose a JPEG or PNG photo.");
                }
                if (reader.getWidth(0) > MAX_PHOTO_DIMENSION || reader.getHeight(0) > MAX_PHOTO_DIMENSION)
                {
                    throw new IOException("Photo dimensions must be at most 4096 × 4096.");
                }
                return new Photo(bytes, preview(reader.read(0), PREVIEW_EDGE));
            }
            finally
            {
                reader.dispose();
            }
        }
    }

    static ImageIcon preview(BufferedImage source, int maxEdge)
    {
        double scale = Math.min(1, (double)maxEdge / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int)Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int)Math.round(source.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = resized.createGraphics();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        }
        finally
        {
            graphics.dispose();
        }
        return new ImageIcon(resized);
    }
}
