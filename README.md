# Spotter - CS 2114 Project 3, Group 82

Spotter is an in-memory console prototype for finding gym partners. Eligible
profiles are queued in FIFO order. Users can swipe, inspect mutual matches,
and exchange messages only with matches. Shared workout routines are a D4
proposal, not an implemented feature.

## Running the model, tests, and console

Use JDK 17. The repository is an Eclipse Java project; `.classpath` declares
JUnit 5. There is no Maven or Gradle build. Run commands from the repository
root. Terminal examples also require `rg` (ripgrep). Do not use old `bin/`
classes as evidence that the current source builds.

### Eclipse

1. Import the folder using File > Import > General > Existing Projects into
   Workspace. Use Eclipse with Java and JUnit 5 support and select JDK 17.
2. Select Project > Clean. Run `LinkedQueueTest`, `ProfileManagerTest`, and
   `MainLogicTest` individually using Run As > JUnit Test for model-only checks.
3. Run `SpotterControllerTest` separately for console/controller integration.
4. Run `SpotterApp.java` using Run As > Java Application for the minimal View.

The command-line workflow below was executed. The Eclipse GUI workflow has
not been independently exercised during this work.

### Model-only execution through JUnit

There is no separate model driver: the tests directly exercise the model.
Use a JUnit Platform Console Standalone JAR, which supplies the existing JUnit
Jupiter framework. No new testing framework is required. On the verified
machine the cached JAR is:

```sh
JUNIT="$HOME/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
```

On another machine, set `JUNIT` to its own Console Standalone JAR. That cache
path is not included in the repository and may not exist on another computer.

```sh
MODEL_OUT=$(mktemp -d /tmp/group82-model.XXXXXX)
javac -d "$MODEL_OUT" -cp "$JUNIT" \
  $(rg --files src/spotter -g '*.java' | rg -v '/(Spotter|ProfileView|MatchesView|ChatView)')
java -jar "$JUNIT" execute --class-path "$MODEL_OUT" \
  --scan-class-path --fail-if-no-tests --details summary
```

This compilation excludes every View, Controller, launcher, and UI test file,
including the separately merged ProfileView, MatchesView, and ChatView.

### Controller tests

```sh
TEST_OUT=$(mktemp -d /tmp/group82-tests.XXXXXX)
javac -d "$TEST_OUT" -cp "$JUNIT" src/spotter/*.java
cp -R src/spotter/photos "$TEST_OUT/spotter/"
java -jar "$JUNIT" execute --class-path "$TEST_OUT" \
  --select-class spotter.SpotterControllerTest --fail-if-no-tests --details summary
```

The copy step puts the four photo files on the runtime classpath. `javac`
compiles Java files but does not copy images. Omitting it causes the two
photo-dependent Swing tests to fail in a fresh output directory.

To run all 65 tests after the same compilation and resource copy:

```sh
java -Djava.awt.headless=true -jar "$JUNIT" execute \
  --class-path "$TEST_OUT" --scan-class-path --fail-if-no-tests --details summary
```

### Minimal console View

The console application does not require JUnit:

```sh
CONSOLE_OUT=$(mktemp -d /tmp/group82-console.XXXXXX)
javac -d "$CONSOLE_OUT" \
  $(rg --files src/spotter -g '*.java' -g '!*Test.java')
java -cp "$CONSOLE_OUT" spotter.SpotterApp
```

The demo starts as Alice (ID 1). Bob (ID 2) and Dave (ID 4) also like
Running; Carol (ID 3) likes both Swimming and Running, so she also appears
in the Running users' candidate queues. Commands:

| Command | Action |
| --- | --- |
| `profile` | Display the current candidate without removing it. |
| `left` / `right` | Swipe, then display the next candidate. |
| `matches` | List mutual matches for the active user. |
| `send <id> <text>` | Send a nonempty message to a match. |
| `chat <id>` | Display an authorized conversation. |
| `user <id>` | Switch between existing demo sessions. This is not login. |
| `help` / `quit` | Show commands or exit. EOF also exits. |

For a presentation, launch the guided demo instead:

```sh
java -cp "$CONSOLE_OUT" spotter.SpotterApp --demo
```

Press Enter to run each of 18 explained steps, or type `quit` to stop. The demo
uses real Controller commands and reciprocal swipes, not seeded matches. Dave
makes queue advancement visible; the guided flow also demonstrates a left swipe,
an empty queue, rejected messaging, shared conversations, and invalid input.
Without `--demo`, the menu and `spotter [user <id>]>` prompt support free input.

For a complete demo, enter:

```text
profile
right
matches
user 2
right
matches
send 1 Hello Alice
user 1
send 2 Hi Bob
chat 2
user 3
chat 1
quit
```

Alice's first like creates no match. Bob's reciprocal like creates the match;
both users can then see the same conversation. Carol cannot see their messages.

### Desktop card-and-chat demo

An optional Java Swing View keeps the same Queue, matching, and messaging
rules and routes buttons through SpotterController. No extra library is needed.
Compile production sources using the console instructions, then launch:

```sh
cp -R src/spotter/photos "$CONSOLE_OUT/spotter/"
java -cp "$CONSOLE_OUT" spotter.SpotterSwingView
```

In Eclipse, run `SpotterSwingView.java` as a Java Application. Like Bob as Alice,
switch the Demo account dropdown to Bob, and like Alice. Select the resulting
match in the list to send a message, then switch back to Alice and select Bob
to see and reply to the same conversation. Carol cannot see that conversation.
Pass and Like disable when a queue is exhausted. The dropdown is demo identity
switching, not authentication. The four fixed demo profiles use user-supplied sample photos: Alice and Carol
use the two female portraits; Bob and Dave use two male portraits. These are
fictional demo assignments, not claims about the people pictured. New profiles
without a photo use their initial.

SpotterApp.createSessions is shared by console and desktop launchers; each new
launch starts fresh in-memory state. The original console remains available.

### Create a demo profile

In the desktop window, click **Create profile**. Enter a name, positive integer
age, workout interest and experience; goal, gym, preferred time and bio are
optional. Invalid name/age inputs keep the form open with an error. Cancel
leaves the sessions unchanged. The app assigns an ID and switches to the new
profile. Existing users see the new candidate if they share a workout interest;
mutual likes are still required for messaging. New accounts share the same
in-memory managers. Closing the application loses created profiles.

The editable Gym dropdown includes 23 facilities from the user-provided
VT/Blacksburg/Christiansburg facility summary, including McComas, War Memorial,
Venture Out, The Weight Club, and the aquatic centers. A custom location or an
empty optional field is also allowed. Activity choices include court sports,
climbing, boxing, yoga, and other sports described in that summary. Facility
names were transcribed from the supplied material, not independently verified;
no opening hours, fees, access guarantees, or facility-based filtering are added.
Sample users' facility choices are fictional demo assignments.

The profile card and console show these additional details. They do not affect
filtering or mutual-match rules. Existing UserProfile constructor callers still
work. The original fixed demo accounts are sample data.

### Verified results

On Java 17.0.12, compilation succeeded and these tests were actually executed:

| Test file | Result | Coverage |
| --- | --- | --- |
| `UserProfileTest.java` | 5/5 passed | Constructor/detail validation, valid and rejected edits, unchanged state after invalid edits, defensive map copies. |
| `LinkedQueueTest.java` | 10/10 passed | FIFO, peek, size/empty state, null rejection, empty exceptions, reuse after draining. |
| `ProfileManagerTest.java` | 7/7 passed | Eligibility, self/duplicate exclusion, FIFO removal, empty access, no re-addition after removal. |
| `MainLogicTest.java` | 11/11 passed | Actual two-user swipes, mutual matches, null/left swipes, history, filtering, empty queues, messaging and unauthorized access. |
| `SpotterControllerTest.java` | 3/3 passed after correction | Console workflow, invalid commands/IDs, empty queues, message rejection, EOF. |
| `SpotterSwingViewTest.java` | 4/4 passed | Actual button-driven reciprocal match/chat, blank-message rejection, account isolation, pass and exhausted queues. |
| `ProfileViewTest.java` | 4/4 passed | Profile formatting, missing fields, swipe options, empty state. |
| `MatchesViewTest.java` | 4/4 passed | Match formatting, empty lists, new-match banner. |
| `ChatViewTest.java` | 5/5 passed | Conversation formatting, missing names, empty history, prompts. |
| `SpotterViewTest.java` | 12/12 passed | Teammate View API, injected input/output, input trimming and EOF. |

The current model-only run passed 33 tests without compiling any View or
Controller code. The current full run passed 65 tests (33 model + 32 UI).
The separate Controller run passed 3 tests. Separate console processes also
completed the two-user workflow, malformed-input/empty-queue checks, and an
immediate EOF check with exit code 0 and no unhandled exception. Inputs included
blank/unknown commands, nonnumeric and overflowing IDs, unknown users, missing
arguments, empty messages, and unmatched messaging. `git diff --check` passed.
The current full run used `-Djava.awt.headless=true`; all 65 tests passed.
The photo-enabled desktop panels were rendered at 960 x 820 for all four
demo profiles and visually inspected. A regression test loads every demo photo
and checks that new/empty profiles clear the previous image.
The tool environment could not launch a native window (exit code 134).
Local manual checks confirmed GUI launch, Bob-to-Carol card/photo advancement,
reciprocal likes, match selection, messages/replies, and account isolation.
These team-reported checks are separate from automated results and do not
establish that every input or UI environment works.

### D3 rubric evidence and final checks

| Criterion | Evidence | Remaining confirmation |
| --- | --- | --- |
| Model Works / Exceeds | LinkedQueueTest: 10 passing Queue tests; 33 model-only tests with no UI compiled. | Team can explain front/rear links and FIFO. |
| Thin View / Exceeds | Console and Swing actions call SpotterController, then MainLogic; model has no UI imports. | Local launch and Bob-to-Carol card/photo advancement manually checked. |
| Runs Without Crashing / Exceeds | Full workflow, null/left/duplicate swipes, exhausted queues, bad commands/IDs, rejected messages and unauthorized reads have passing tests. | Jaden completed manual mutual matching, messaging/replies, and Carol account-isolation checks (team-confirmed). |
| Reflection / Exceeds | Each implementation deviation below names its actual trigger or test failure. | Team factual review. |
| Next Steps / Exceeds | Shared-workout proposal names methods, unchanged classes, normal/bad-input tests, and confirmed owners. | D4 feature is planned, not implemented. |
| Attribution + GenAI | Every current Java file has a row; Original implementation, AI-assisted changes, and manual testing are distinguished. | Completed contributions are listed per file, including Jaden's team-reported manual integration tests; planned D4 work remains separate. |

Bad-input evidence: LinkedQueueTest covers null and empty operations;
ProfileManagerTest covers self/duplicates/filtering/empty access;
UserProfileTest covers invalid construction and rejected edits preserving state;
MainLogicTest covers null swipes preserving queue/history, duplicate decisions,
empty queues/messages and unmatched conversation access;
SpotterControllerTest covers malformed commands/IDs and EOF. These tests prove
the named cases, not exhaustive input coverage or authenticated user security.

## Reflection

### How did your design absorb the change?

Our MVC design made it easier to add workout functionality without affecting 
existing matching and messaging features. As predicted in D3, we created 
Workout and WorkoutManager and modified MainLogic, SpotterController, and
SpotterView. We also added WorkoutTest and WorkoutManagerTest and expanded our
existing tests. The existing structure allowed us to integrate the feature
without major changes to other classes. 

### Did the change cross the model/view line?

The workout feature required changes to the model, controller, and view, but 
maintained MVC separation. Workout and WorkoutManager handle workout data, 
MainLogic coordinates the functionality, and SpotterController processes user 
commands. We updated the console menu in SpotterView and reused its existing 
display methods. We are also developing WorkoutView and dedicated showWorkout() 
methods for SpotterView and SpotterSwingView to improve workout display.

### What changed from your D3 plan?

Our original D3 plan proposed shared workouts containing exercises, sets,
repetitions, and weights. For this prototype, we implemented individual workout
creation, exercise management, and workout viewing using three console
commands: createworkout, addexercise, and workout. We also added input
validation and additional JUnit tests. We are continuing to develop shared
workout functionality as part of our final implementation. 

### What doesn't work yet?

The workout feature currently supports individual routines but does not yet
include sets, repetitions, weights, or workout sharing. Workouts are stored in
memory and are not saved between sessions. We are currently working on shared
workout functionality, dedicated workout views, an Android application, and
live chatting. These features still need to be completed and tested. Our 
current JUnit test suite passes, but further integration testing will be 
necessary.

## Next Steps

### What is left for the final build?

Our profile matching, messaging, and individual workout features are working
and have passed our current JUnit tests. Our next priority is completing shared
workout functionality and integrating the new workout views. We are also 
implementing the Spotter application, including profile browsing, matching,
and messaging, along with live chatting. Once these features are finished, we 
will test their integration, fix remaining bugs, and prepare the final 
demonstration. 

### Stretch Goals

We are continuing to pursue shared workout routines, an Android application,
and live chatting. We are dropping distance-based matchmaking and workout
history so we can focus on completing our current features. 

### 3 Minute Demo

We will start by showing how users browse profiles, swipe right, and create
mutual matches. Next, we will demonstrate messaging between matched users. We
will then create a "Push Day" workout, add "Bench Press" and "Shoulder Press,"
and display the exercises. If completed and tested, we will also demonstrate 
shared workouts, the Android application, and live chatting. Finally, we will 
briefly demonstrate input validation.

### Who does what?

Gowri is responsible for individual workout functionality, model and controller
integration, and unit testing. Varooni is assigned to the workout views and
interface testing. Our team is also working on shared workout functionality, 
Android development, and live chatting. We will coordinate integration, 
debugging, final testing, documentation, and the demonstration as a group. 
The specific owners of the remaining features will be confirmed before 
submission.

## Attribution

The table records original implementation, Codex-assisted changes, and manual
testing. Codex generated/revised code and executed automated checks under team
direction. Team members confirmed their attribution entries and D4 roles;
this confirmation is separate from a detailed source-code review. AI-assisted
work is labeled explicitly.

All paths below are under `src/spotter/`. This table includes every current
class/enum and test file. LinkedQueue's private Node class is included in its
file's row.

| File | Actual contribution supported by available information |
| --- | --- |
| UserProfile.java | Gowri: original implementation (team-confirmed). David (Codex-assisted): added optional goal/gym/time/bio fields and compatible constructor; tested validation. |
| UserProfileTest.java | David (Codex-assisted): generated and executed 5 constructor, edit-validation, and defensive-copy tests. |
| LinkedQueue.java | Gowri: original implementation including Node (team-confirmed). David (Codex-assisted): inspected; no production changes; directed inspection and automated test execution. |
| ProfileManager.java | Gowri: original implementation (team-confirmed). David (Codex-assisted): revised addProfile return value and checked callers/tests. |
| LinkedQueueTest.java | Gowri: original tests (team-confirmed). David (Codex-assisted): added two edge-case tests and executed suite. |
| ProfileManagerTest.java | Gowri: original tests (team-confirmed). David (Codex-assisted): added return-value assertions and executed suite. |
| SwipeAction.java | David (Codex-assisted): generated LEFT/RIGHT enum; exercised through workflow tests. |
| MainLogic.java | David (Codex-assisted): generated model orchestration; exercised through model and Controller tests. |
| MatchManager.java | David (Codex-assisted): generated shared swipe/mutual-like state; exercised through workflow tests. |
| ChatManager.java | David (Codex-assisted): generated matched-only messaging and copied conversation output; exercised through workflow tests. |
| Message.java | David (Codex-assisted): generated immutable sender/content/timestamp fields and content validation; used by messaging tests. |
| MainLogicTest.java | David (Codex-assisted): generated and executed 11 workflow tests. |
| SpotterView.java | David (Codex-assisted): implemented console View and compatibility methods for Varooni's formatter/input APIs; exercised integration tests/process runs. |
| SpotterController.java | David (Codex-assisted): generated command routing/parsing; corrected map null check after failing tests; executed integration checks. Jaden: manually tested mutual matching, messaging/replies, and account isolation (team-confirmed). |
| SpotterApp.java | David (Codex-assisted): generated demo wiring; executed separate console processes. |
| SpotterSwingView.java | David (Codex-assisted): implemented Swing card/chat View and automated rendering/button checks. David: manually confirmed local launch and Bob-to-Carol card/photo advancement. Jaden: manually tested match selection, messaging/replies, account switching, and Carol conversation isolation (team-confirmed). |
| SpotterSwingViewTest.java | David (Codex-assisted): generated and executed 4 desktop interaction tests, including creating a profile and matching/messaging with an existing account. |
| SpotterControllerTest.java | David (Codex-assisted): generated and executed 3 integration tests. |
| ProfileView.java | Varooni: authored (team-confirmed). David (Codex-assisted): integrated existing formatter API. |
| MatchesView.java | Varooni: authored (team-confirmed). David (Codex-assisted): integrated existing formatter API. |
| ChatView.java | Varooni: authored (team-confirmed). David (Codex-assisted): integrated existing formatter API. |
| ProfileViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): enabled Jupiter annotations and executed 4 tests. |
| MatchesViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): enabled Jupiter annotations and executed 4 tests. |
| ChatViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): enabled Jupiter annotations and executed 5 tests. |
| SpotterViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): adapted console assertions, enabled Jupiter annotations, and executed 12 tests. |

The .classpath and README updates were also Codex-assisted. A pre-existing
.project resource-filter edit was preserved. Planned responsibilities are
separate from completed contributions.

## GenAI usage note

