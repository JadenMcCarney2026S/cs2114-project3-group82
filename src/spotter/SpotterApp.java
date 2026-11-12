package spotter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SpotterApp
{
    public static void main(String[] args)
    {
        Map<Integer, MainLogic> sessions = createSessions();
        SpotterView view = new SpotterView(System.out);
        view.showInfo("Spotter demo: 1 Alice, 2 Bob, 3 Carol, 4 Dave. Active user: 1");
        view.showInfo("Alice, Bob, Dave: Running | Carol: Swimming");
        SpotterController controller = new SpotterController(sessions, 1, view);
        Scanner input = new Scanner(System.in);
        if (args.length == 1 && args[0].equals("--demo"))
        {
            runGuidedDemo(controller, view, input);
        }
        else
        {
            controller.run(input);
        }
    }

    static Map<Integer, MainLogic> createSessions()
    {
        List<UserProfile> users = List.of(
            new UserProfile(1, "Alice", 20, Map.of("Running", "Beginner"),
                "Build endurance", "Campus gym", "Morning", "Looking for a steady workout partner."),
            new UserProfile(2, "Bob", 21, Map.of("Running", "Intermediate"),
                "Train for a 10K", "Campus gym", "Evening", "Easy runs and good company."),
            new UserProfile(3, "Carol", 22, Map.of("Swimming", "Beginner"),
                "Improve technique", "Aquatic center", "Afternoon", "Learning one lap at a time."),
            new UserProfile(4, "Dave", 23, Map.of("Running", "Beginner"),
                "Stay consistent", "Community gym", "Weekend", "Always up for a relaxed run."));
        ChatManager chat = new ChatManager(new MatchManager());
        Map<Integer, MainLogic> sessions = new LinkedHashMap<>();
        for (UserProfile user : users)
        {
            MainLogic model = new MainLogic(user, chat);
            for (UserProfile candidate : users)
            {
                model.addProfile(candidate);
            }
            sessions.put(user.getUserId(), model);
        }
        return sessions;
    }

    private static void runGuidedDemo(SpotterController controller,
        SpotterView view, Scanner input)
    {
        String[][] steps = {
            {"Retrieve Bob without consuming the queue", "profile"},
            {"A message before matching must be rejected", "send 2 Hello"},
            {"Alice likes Bob; Dave becomes the next profile", "right"},
            {"One-sided like does not create a match", "matches"},
            {"Alice passes Dave; the queue is now empty", "left"},
            {"Swiping an empty queue is safe", "right"},
            {"Switch to Bob's independent session", "user 2"},
            {"Bob likes Alice: a real mutual match is created", "right"},
            {"Inspect the mutual match", "matches"},
            {"Bob sends Alice a message", "send 1 Hello Alice"},
            {"Return to Alice's existing session", "user 1"},
            {"Alice can also see the match", "matches"},
            {"Alice replies", "send 2 Hi Bob"},
            {"Read both messages", "chat 2"},
            {"Carol has no shared interests with these candidates", "user 3"},
            {"Carol cannot read Alice and Bob's messages", "chat 1"},
            {"Malformed IDs produce an error, not a crash", "user abc"}
        };
        view.showInfo("\nGUIDED DEMO - press Enter for each step; type quit to stop.");
        for (int i = 0; i < steps.length; i++)
        {
            view.showInfo("\n[" + (i + 1) + "/" + steps.length + "] " + steps[i][0]);
            view.showInfo("Command: " + steps[i][1] + "  (Enter to run)");
            if (!input.hasNextLine() || input.nextLine().trim().equalsIgnoreCase("quit"))
            {
                view.showInfo("Demo stopped.");
                return;
            }
            controller.handleCommand(steps[i][1]);
        }
        view.showInfo("\nDemo complete. Restart without --demo for free input.");
    }
}
