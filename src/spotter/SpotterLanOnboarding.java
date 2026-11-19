package spotter;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FlowLayout;
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
    private static final String ACCOUNT_GUIDANCE =
        "Use 3–32 letters, digits, or underscores for your username and 12–128 characters for your password.";
    private static final String PHOTO_GUIDANCE =
        "Choose a JPEG or PNG up to 2 MB and no larger than 4096 × 4096 pixels.";
    private static final String PROFILE_GUIDANCE =
        "Add a display name, an age of 18 or older, a workout interest, and a photo.";

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
        status.setForeground(SpotterLanStyle.MUTED);
        status.setText(notice);
        showLogin();
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setLocationByPlatform(true);
        window.setVisible(true);
    }

    private void showLogin()
    {
        JPanel form = form();
        addField(form, "Username", username);
        addField(form, "Password", password);
        JPanel network = form();
        addField(network, "Server URL", address);
        network.setVisible(false);
        JToggleButton settings = new JToggleButton("Connection settings");
        settings.setHorizontalAlignment(SwingConstants.LEADING);
        settings.addActionListener(event -> {
            network.setVisible(settings.isSelected());
            window.pack();
            if (settings.isSelected())
            {
                address.requestFocusInWindow();
            }
        });
        JPanel actions = actions();
        JLabel requirements = new JLabel("<html>New accounts need a username of 3–32 letters, digits,"
            + " or underscores<br>and a password of 12–128 characters.</html>");
        requirements.setForeground(SpotterLanStyle.MUTED);
        requirements.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JButton login = new JButton("Log in");
        JButton signUp = new JButton("Sign up");
        SpotterLanStyle.button(login, true);
        SpotterLanStyle.button(signUp, false);
        actions.add(signUp);
        actions.add(login);
        login.addActionListener(event -> authenticate(false, login, signUp, settings));
        signUp.addActionListener(event -> authenticate(true, login, signUp, settings));
        JPanel access = new JPanel(new BorderLayout(0, 12));
        access.setOpaque(false);
        access.add(form, BorderLayout.NORTH);
        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(requirements);
        options.add(Box.createVerticalStrut(12));
        options.add(settings);
        options.add(network);
        access.add(options, BorderLayout.CENTER);
        show("Welcome to Spotter", access, actions, login);
    }

    private void authenticate(boolean signUp, JButton login, JButton register, JToggleButton settings)
    {
        SpotterLanClient client;
        try
        {
            serverAddress = URI.create(address.getText().trim());
            client = new SpotterLanClient(serverAddress);
        }
        catch (IllegalArgumentException error)
        {
            showError("Check the server address", "The server URL is invalid.",
                "Enter the HTTPS address provided by your host, ending in /chat.");
            if (!settings.isSelected())
            {
                settings.doClick(0);
            }
            address.requestFocusInWindow();
            return;
        }
        String name = username.getText().trim();
        char[] entry = password.getPassword();
        password.setText("");
        if (name.isEmpty() || entry.length == 0)
        {
            Arrays.fill(entry, '\0');
            showError("Enter your account details", "A username and password are required.",
                signUp ? ACCOUNT_GUIDANCE : "Enter the username and password you used to sign up.");
            (name.isEmpty() ? username : password).requestFocusInWindow();
            return;
        }
        work("Connecting…", signUp ? "Couldn’t sign up" : "Couldn’t log in",
            signUp ? ACCOUNT_GUIDANCE + " If you already have an account, log in instead. "
                + "If the server is unavailable, confirm the host’s HTTPS address."
                : "Check your username and password. If the server is unavailable, confirm the host’s HTTPS address.",
            () -> {
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
        preview.setForeground(SpotterLanStyle.MUTED);
        preview.setOpaque(true);
        preview.setBackground(SpotterLanStyle.SURFACE);
        preview.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        JPanel actions = actions();
        JButton choose = new JButton("Choose photo");
        JButton save = new JButton("Save profile");
        JButton cancel = new JButton("Log out");
        SpotterLanStyle.button(choose, false);
        SpotterLanStyle.button(save, true);
        SpotterLanStyle.button(cancel, false);
        actions.add(cancel);
        actions.add(choose);
        actions.add(save);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(preview, BorderLayout.CENTER);
        choose.addActionListener(event -> choosePhoto(preview, choose, save, cancel));
        save.addActionListener(event -> saveProfile(client, displayName.getText(), age.getText(),
            interest.getText(), bio.getText(), choose, save, cancel));
        cancel.addActionListener(event -> work("Logging out…", "Couldn’t reach the server",
            "Check your connection, then try logging out again.", () -> {
            client.logout();
            return null;
        }, ignored -> backToLogin(), choose, save, cancel));
        show("Build your profile", content, actions, save);
    }

    private void choosePhoto(JLabel preview, JButton... buttons)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JPEG and PNG images", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION)
        {
            Path path = chooser.getSelectedFile().toPath();
            work("Loading photo…", "Couldn’t open that photo", PHOTO_GUIDANCE,
                () -> readPhoto(path), photo -> {
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
            showError("Check your age", "Enter your age as a whole number.",
                "You must be 18 or older to create a profile.");
            return;
        }
        if (age < SpotterAccounts.MIN_AGE)
        {
            showError("Check your age", "You must be 18 or older to create a profile.",
                "Check that you entered your correct age.");
            return;
        }
        if (photo == null || name.isBlank() || interest.isBlank())
        {
            showError("Finish your profile", "Some required details are missing.", PROFILE_GUIDANCE);
            return;
        }
        byte[] uploaded = photo.bytes();
        work("Saving profile…", "Couldn’t save your profile", PROFILE_GUIDANCE + " " + PHOTO_GUIDANCE,
            () -> client.completeProfile(name.trim(), age, interest.trim(),
            bio.trim(), uploaded), account -> {
                if (account.profileComplete())
                {
                    openDiscovery(client);
                }
                else
                {
                    showError("Finish your profile", "Profile setup is incomplete.", PROFILE_GUIDANCE);
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

    private void showError(String title, String detail, String guidance)
    {
        status.setText(title);
        SpotterLanStyle.alert(window, title, detail, guidance);
    }

    private <T> void work(String progress, String errorTitle, String guidance,
        Callable<T> task, Consumer<T> onSuccess,
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
                        showError("Session expired", "Your session has ended.",
                            "Log in again to continue setting up your profile.");
                    }
                    else
                    {
                        showError(errorTitle, cause.getMessage(), guidance);
                    }
                }
                catch (InterruptedException error)
                {
                    Thread.currentThread().interrupt();
                    showError(errorTitle, "The request was interrupted.", guidance);
                }
            }
        }.execute();
    }

    private void show(String title, JComponent fields, JComponent actions, JButton submit)
    {
        JPanel body = new JPanel(new BorderLayout());
        body.add(trackPanel(), BorderLayout.WEST);
        JPanel card = new JPanel(new BorderLayout(0, 22));
        card.setBackground(SpotterLanStyle.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 24, 32));
        card.add(SpotterLanStyle.heading(title), BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(actions, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);
        body.add(card, BorderLayout.CENTER);
        window.setContentPane(body);
        window.getRootPane().setDefaultButton(submit);
        window.pack();
    }

    private static JPanel trackPanel()
    {
        JPanel panel = new JPanel(new BorderLayout())
        {
            @Override
            protected void paintComponent(Graphics graphics)
            {
                super.paintComponent(graphics);
                Graphics2D track = (Graphics2D)graphics.create();
                try
                {
                    track.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                    track.setColor(new Color(162, 205, 68));
                    track.setStroke(new BasicStroke(2f));
                    for (int lane = 0; lane < 7; lane++)
                    {
                        int offset = lane * 19;
                        track.drawRoundRect(72 - offset, getHeight() - 216 - offset,
                            340 + offset * 2, 475 + offset * 2, 230 + offset * 2,
                            230 + offset * 2);
                    }
                }
                finally
                {
                    track.dispose();
                }
            }
        };
        panel.setPreferredSize(new Dimension(258, 410));
        panel.setBackground(SpotterLanStyle.ACCENT);
        panel.setBorder(BorderFactory.createEmptyBorder(26, 26, 24, 20));
        JLabel brand = new JLabel("SPOTTER");
        brand.setForeground(SpotterLanStyle.INK);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD | Font.ITALIC, 19f));
        panel.add(brand, BorderLayout.NORTH);
        JLabel headline = new JLabel("<html>Find your<br>training partner.</html>");
        headline.setForeground(SpotterLanStyle.INK);
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 34f));
        panel.add(headline, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel form()
    {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 5));
        form.setOpaque(false);
        return form;
    }

    private static JPanel actions()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    private static void addField(JPanel form, String name, JTextField field)
    {
        JLabel label = new JLabel(name);
        label.setForeground(SpotterLanStyle.INK);
        label.setLabelFor(field);
        SpotterLanStyle.input(field);
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.add(label, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        form.add(row);
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
