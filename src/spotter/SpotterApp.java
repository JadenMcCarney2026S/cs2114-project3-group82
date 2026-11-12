package spotter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SpotterApp
{
    public static void main(String[] args)
    {
        List<UserProfile> users = List.of(
            new UserProfile(1, "Alice", 20, Map.of("Running", "Beginner")),
            new UserProfile(2, "Bob", 21, Map.of("Running", "Intermediate")),
            new UserProfile(3, "Carol", 22, Map.of("Swimming", "Beginner")));
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
        SpotterView view = new SpotterView(System.out);
        view.showInfo("Spotter demo: 1 Alice, 2 Bob, 3 Carol. Active user: 1");
        SpotterController controller = new SpotterController(sessions, 1, view);
        controller.run(new Scanner(System.in));
    }
}
