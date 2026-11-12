package spotter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared swipe state for all users in one application session. */
public class MatchManager
{
    private final Map<Integer, Set<Integer>> swipeHistory = new HashMap<>();
    private final Map<Integer, Set<Integer>> likes = new HashMap<>();

    public boolean hasSwiped(int userId, int otherUserId)
    {
        return swipeHistory.getOrDefault(userId, Set.of()).contains(otherUserId);
    }

    /** Returns true only when this swipe creates a mutual match. */
    public boolean recordSwipe(int userId, int otherUserId, SwipeAction action)
    {
        if (action == null || userId == otherUserId
            || hasSwiped(userId, otherUserId))
        {
            return false;
        }
        swipeHistory.computeIfAbsent(userId, key -> new HashSet<>()).add(otherUserId);
        if (action == SwipeAction.RIGHT)
        {
            likes.computeIfAbsent(userId, key -> new HashSet<>()).add(otherUserId);
            return areMatched(userId, otherUserId);
        }
        return false;
    }

    public boolean areMatched(int userId, int otherUserId)
    {
        return likes.getOrDefault(userId, Set.of()).contains(otherUserId)
            && likes.getOrDefault(otherUserId, Set.of()).contains(userId);
    }
}
