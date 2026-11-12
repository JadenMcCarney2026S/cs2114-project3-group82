# cs2114-project3-group82
Gym partner finder

## Run the independent model tests

Use JDK 17 and Eclipse with Java and JUnit 5 support. Import this folder using
File > Import > General > Existing Projects into Workspace. The project
classpath includes the Eclipse JUnit 5 library.

Select Project > Clean to rebuild, then right-click each of
`src/spotter/LinkedQueueTest.java`, `src/spotter/ProfileManagerTest.java`, and
`src/spotter/MainLogicTest.java` and select Run As > JUnit Test. There are
10 Queue tests, 7 profile-manager tests, and 11 core-workflow tests.
They exercise the model directly without a View or Controller.

## Run the console prototype (Step 6)

In Eclipse, right-click `src/spotter/SpotterApp.java` and select Run As > Java
Application. Alternatively, from the repository root with JDK 17:

```sh
mkdir -p /tmp/group82-console
javac -d /tmp/group82-console $(rg --files src/spotter -g '*.java' -g '!*Test.java')
java -cp /tmp/group82-console spotter.SpotterApp
```

The sample users are 1 Alice and 2 Bob (Running), and 3 Carol (Swimming).
Start as Alice: `profile`, `right`, `user 2`, `right`, `matches`,
`send 1 Hello Alice`, `user 1`, `chat 2`. Both users must actually like each
other before messaging succeeds. `left`, `help`, and `quit` are also available.
`user <id>` switches existing demo sessions, not authenticated accounts.
Invalid commands and IDs display errors; exhausted queues display a message.
EOF exits cleanly. State lasts only until the program exits.

Run `SpotterControllerTest` as a JUnit test separately for console/controller
integration coverage. The 28 model-only tests above remain independent of the
View and Controller.

## Core model workflow (Step 5)

Create one `MatchManager`, then one `ChatManager(matchManager)`. Pass that same
ChatManager to each user's `MainLogic(user, chatManager)` so actual reciprocal
swipes and messages share state. `MainLogic(user)` creates an isolated model.
`MainLogicTest` demonstrates profile retrieval, swiping to the next profile,
mutual matching, and authorized two-way messaging without a View.

Specification clarifications:
- Messaging methods use integer user IDs, matching `UserProfile`, instead of
  the specification's inconsistent String ID parameters.
- `swipeCurrentProfile` returns the newly matched profile, or null if no new
  match was created. Null also covers invalid actions and empty queues; it is
  not a swipe-success flag. LEFT and RIGHT are the supported actions.
- `ProfileManager.addProfile` now returns whether it enqueued the profile, so
  MainLogic can reuse its eligibility checks. Existing statement-style callers
  still work. ProfileManager still throws for null; MainLogic returns false.
- Unauthorized conversation reads return an empty list; returned lists are
  copies. Chat history is formatted as `senderId: content`.

Remaining limits: state is in memory only. A newly constructed MainLogic for
an existing user retains shared swipe history and conversation access, but does
not restore its local profile catalog or matched-profile list. There is no
profile repository/refresh operation or login/authentication. Step 6 adds a
minimal console View and Controller; the model remains independent of them.
The caller supplies the current user identity; this is a local model prototype.
The D4 shared-workout feature remains unimplemented.

REFLECTION

During Deliverable 3, we implemented the Profile & Queue Model using 
UserProfile, LinkedQueue, and ProfileManager. While the original specification 
already included FIFO ordering, workout-interest filtering, and duplicate 
prevention, we refined the implementation by using a HashSet to track 
previously added profiles and passing the current user into the ProfileManager 
constructor. We also used a Map<String, String> to store workout activities and 
experience levels instead of relying on a single workout preference. 
Validation remained within the Model, and JUnit tests confirmed that the 
Profile & Queue components function independently without the View or 
Controller. We resolved how to prevent previously swiped profiles from 
reappearing, but integrating mutual matching, messaging, and data storage 
remains unverified. The complete MVP workflow and its end-to-end testing are 
still unfinished.

NEXT STEPS

For Deliverable 4, our first priority is to finish integrating the Model, View, 
and Controller. We will connect ProfileManager.getNextProfile() and 
removeNextProfile() to the swiping functionality, integrate MatchManager for 
mutual matching, and connect ChatManager for messaging. We will then test the 
complete MVP and fix any integration errors. 

We plan to add shared workout routines so matched users can create and view 
workout plans. We predict this will require adding methods such as 
Workout.addExercise() and Workout.getExercises(), along with new Controller 
methods for handling workout actions and View methods for displaying routines. 
The LinkedQueue, ProfileManager, and their existing methods should not need 
changes because workout routines are separate from profile filtering. 

Stretch Goals: We will attempt shared workout routines and drop distance-based 
matchmaking and workout history for Deliverable 4 to prioritize completing 
the MVP.

Testing: We will add JUnit tests for MainLogic.swipeCurrentProfile(), 
getMatches(), sendMessage(), and getConversation(), covering mutual matches, 
duplicate swipes, empty messages, and unauthorized conversations. We will also 
test ProfileManager.removeNextProfile() when the queue becozmes empty and the 
new workout methods with valid and invalid exercise information. 

Division of Work: David will handle Profile & Queue Model fixes and related 
tests. Gowri will complete matching, messaging, and their tests. Varooni will 
handle the View updates and interface testing. Jaden will integrate the 
Controller, connect the components, and test the complete application.

