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
  $(rg --files src/spotter -g '*.java' | rg -v '/Spotter')
java -jar "$JUNIT" execute --class-path "$MODEL_OUT" \
  --scan-class-path --fail-if-no-tests --details summary
```

This compilation excludes every `Spotter*` file, including the View,
Controller, application launcher, and Controller tests.

### Controller tests

```sh
TEST_OUT=$(mktemp -d /tmp/group82-tests.XXXXXX)
javac -d "$TEST_OUT" -cp "$JUNIT" src/spotter/*.java
java -jar "$JUNIT" execute --class-path "$TEST_OUT" \
  --select-class spotter.SpotterControllerTest --fail-if-no-tests --details summary
```

To run all tests, use the same output directory and replace `--select-class
spotter.SpotterControllerTest` with `--scan-class-path`.

### Minimal console View

The console application does not require JUnit:

```sh
CONSOLE_OUT=$(mktemp -d /tmp/group82-console.XXXXXX)
javac -d "$CONSOLE_OUT" \
  $(rg --files src/spotter -g '*.java' -g '!*Test.java')
java -cp "$CONSOLE_OUT" spotter.SpotterApp
```

The demo starts as Alice (ID 1). Bob (ID 2) and Dave (ID 4) also like
Running; Carol (ID 3) likes Swimming and has no eligible candidates in this sample. Commands:

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

Press Enter to run each of 17 explained steps, or type `quit` to stop. The demo
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
java -cp "$CONSOLE_OUT" spotter.SpotterSwingView
```

In Eclipse, run `SpotterSwingView.java` as a Java Application. Like Bob as Alice,
switch the Demo account dropdown to Bob, and like Alice. Select the resulting
match in the list to send a message, then switch back to Alice and select Bob
to see and reply to the same conversation. Carol cannot see that conversation.
Pass and Like disable when a queue is exhausted. The dropdown is demo identity
switching, not authentication. Profiles use initial avatars, not photographs.

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
| `UserProfileTest.java` | 2/2 passed | Optional profile information, original constructor compatibility, invalid name/age/null detail rejection. |
| `LinkedQueueTest.java` | 10/10 passed | FIFO, peek, size/empty state, null rejection, empty exceptions, reuse after draining. |
| `ProfileManagerTest.java` | 7/7 passed | Eligibility, self/duplicate exclusion, FIFO removal, empty access, no re-addition after removal. |
| `MainLogicTest.java` | 11/11 passed | Actual two-user swipes, mutual matches, null/left swipes, history, filtering, empty queues, messaging and unauthorized access. |
| `SpotterControllerTest.java` | 3/3 passed after correction | Console workflow, invalid commands/IDs, empty queues, message rejection, EOF. |
| `SpotterSwingViewTest.java` | 3/3 passed | Actual button-driven reciprocal match/chat, blank-message rejection, account isolation, pass and exhausted queues. |

The earlier model-only run passed 28 tests without compiling View or Controller code.
The separate Controller run passed 3 tests. Separate console processes also
completed the two-user workflow, malformed-input/empty-queue checks, and an
immediate EOF check with exit code 0 and no unhandled exception. Inputs included
blank/unknown commands, nonnumeric and overflowing IDs, unknown users, missing
arguments, empty messages, and unmatched messaging. `git diff --check` passed.
After adding the desktop View, all 36 tests passed with `-Djava.awt.headless=true`.
The desktop panel was also rendered at 960 x 650 and visually inspected.
Launching the native JFrame from the tool environment terminated with exit code
134 and no diagnostic output; native window interaction therefore remains
unverified. Run it in the local IDE/desktop to check the actual window. These results do not
establish that every possible input or UI environment works.

## Reflection

### Specification, implementation, and reasons for changes

| Specification / starting point | What was built and why | Evidence or triggering problem |
| --- | --- | --- |
| A singly linked FIFO queue independent of the View. | Reused `LinkedQueue` unchanged; added populated-null and drain/refill tests. | Inspection found correct queue code but no explicit drain/refill or populated-null regression test. Both new tests passed. |
| JUnit assertions exercise the model. | Added Eclipse's JUnit 5 container to `.classpath`. | Baseline compilation without JUnit failed with unresolved Jupiter imports. Supplying cached JUnit compiled successfully and ran all 15 original tests. |
| Profile eligibility and duplicate prevention belong in the model. | Reused `ProfileManager`; changed `addProfile` from void to boolean so MainLogic can know whether it queued a candidate without duplicating filtering. Null still throws here; MainLogic rejects null with false. | Integration needed an acceptance result. Existing statement-style callers still compile; tests now assert acceptance and rejection. |
| MainLogic coordinates swipes, mutual matching, and messaging. | Added the previously absent MainLogic, MatchManager, ChatManager, Message, and SwipeAction. Users share one ChatManager and its MatchManager. | The initial checkout contained only the profile/queue model. Two isolated per-user stores could not see reciprocal likes. Tests now create matches through actual swipes from two users. |
| Messaging signatures use String IDs, while profile IDs and swipe maps use integers. | Messaging APIs use integer IDs consistently. | Inspection exposed conflicting ID types; no existing messaging callers needed migration. |
| A swipe returns a newly matched profile or null. | Preserved this contract. Null can mean a valid nonmatching swipe, invalid action, or empty queue. The Controller checks action/queue before displaying an outcome. | Specification review showed null cannot be treated as a general swipe-success flag. Null-action, left-swipe, and empty-queue tests cover the separate cases. |
| Conversations are available only to matches. | Model authorization checks reciprocal likes. Unauthorized reads return an empty list; results are copies. Messages include sender, content, and timestamp; console history displays sender and text. | Tests show unmatched writes store nothing and an outsider cannot read an existing private conversation. |
| SpotterView coordinates separate profile, match, and chat View classes; Controller takes one model and View. | One thin SpotterView formats all three outputs. SpotterController accepts existing user sessions, active ID, and View; SpotterApp wires sample profiles and shared state. | No View existed to reuse. A minimal console and session switching demonstrate both real users without extra presentation classes or hardcoded matches. |
| Console input should not crash the MVP. | Controller parses commands and integer IDs; model still enforces business rules. | The first Controller test run failed 3/3 because `Map.of(...).containsValue(null)` throws. Replaced that null check; all 3 tests then passed. Process checks also exercised bad input and EOF. |

The workout-experience map, FIFO filtering, and duplicate prevention already
appear in the supplied specification. They are not new specification changes.
The initial profile model implements the specification's later data/state
section, not its earlier, broader list of gender, location, and workout fields.

### Independence and unknowns

The model imports no View or Controller code. The 28 model tests compile and
execute with all `Spotter*` sources excluded. Input parsing belongs to the
Controller; queue rules, eligibility, swipe history, matching, and conversation
authorization remain in the model. No model rule had to move into the View.

Resolved technical questions include FIFO profile consumption, preventing
previously swiped IDs from reappearing, sharing reciprocal-like state, and
restricting stored conversations to matches. These are supported by executed
tests, not seeded matches. Session switching in the console keeps the existing
MainLogic instances rather than rebuilding them.

Still unresolved: rebuilding a user's MainLogic with shared managers preserves
swipe/conversation state but does not restore its local profile catalog or
matched-profile list. `refreshProfileQueue()` and a profile repository are
absent. State is not persisted; identity is supplied by the local demo rather
than authentication. Shared workouts are not implemented. The complete original
scope document was not supplied, so its full original unknown list remains
unconfirmed; the supplied revised specification is the available reference.

### Unfinished work and test limits

The implemented console MVP path has been exercised. The desktop now creates profiles; console creation and profile editing remain
absent. It does not provide persistent storage, authentication, location
matching, workout history, or shared routines. Separate ProfileView,
MatchesView, and ChatView classes are absent intentionally.

No known executed test remains failing after the Controller correction.
There are no dedicated Message, MatchManager, or ChatManager test
files; MainLogic tests cover only some of their behavior. Constructor map-entry
validation, direct profile-edit validation, timestamp behavior, and restoring
matched-profile lists after rebuilding a session lack dedicated regression
tests. Eclipse GUI execution and exhaustive input testing were not performed.

## Next Steps

### Remaining prototype work in implementation order

The demonstrated fixed-session console flow works. The remaining work below
addresses known gaps and final delivery; optional scope decisions should be
confirmed before expanding the MVP. Owners are **TBD**, as the user confirmed
that assignments are not yet known. Specification role assignments are plans,
not evidence that someone performed or accepted the work.

| Order | Task and expected methods/classes | Verification | Confirmed owner |
| --- | --- | --- | --- |
| 1 | Human review of the generated model/UI changes and completion of actual review/test attribution in this README. | Team can explain the queue, reciprocal match state, and access checks; verify attribution per file. | TBD |
| 2 | Decide whether reconstructing a session is part of the required MVP; if so, restore known profiles/matches through MainLogic constructor/getMatches and shared profile storage, without requeueing swiped IDs. Define profile source before adding refreshProfileQueue. | A matched user rebuilt with shared state still lists the match and conversation; swiped/ineligible candidates remain excluded. | TBD |
| 3 | Add focused validation and regression tests for UserProfile constructor/setName/setAge/addWorkoutInterest, Message, and any approved restoration change. | Valid inputs succeed; blank/null fields, invalid ages, duplicates, and unauthorized access preserve existing state. | TBD |
| 4 | Final clean build, model-only tests, Controller tests, console smoke run, and Eclipse launch on the team's submission machine. Reconcile README with those results. | Document observed outcomes; resolve failures before submission. | TBD |

The specification proposed David for profiles/Queue, Gowri for matching and
messaging, Varooni for Views, and Jaden for Controller work. These are reference
roles only; no remaining-task owner is inferred from them.

### Proposed D4 change feature: shared workout routines

Plan only: matched users can create and inspect a shared routine containing
exercise names, sets, repetitions, and weights. This adds model state and
validation and is reachable through Controller and View, rather than a cosmetic
change. It was optional in the specification and is not part of this built MVP.

| Layer | Expected change (proposed names, not existing APIs) | Confirmed owner |
| --- | --- | --- |
| Model | New Workout.addExercise and Workout.getExercises; a pair-keyed WorkoutManager for shared state/access; MainLogic.addWorkoutExercise and MainLogic.getWorkout to authorize and coordinate calls. | TBD |
| Controller | SpotterController.handleCommand plus addWorkoutExercise/displayWorkout actions to parse exercise inputs and invoke MainLogic. | TBD |
| View | SpotterView.showWorkout to display routine snapshots and validation feedback. | TBD |
| Wiring | SpotterApp.main to create and share workout state across existing user sessions. | TBD |
| Tests | New WorkoutTest/WorkoutManagerTest; extend MainLogicTest and SpotterControllerTest for the reachable shared-routine flow. | TBD |

Expected to remain unchanged: LinkedQueue.enqueue/dequeue/peek/isEmpty/getSize,
ProfileManager.addProfile/getNextProfile/removeNextProfile, UserProfile,
Message, ChatManager.sendMessage/getConversation, and MatchManager's existing
swipe/match rules. Workout authorization should reuse MatchManager.areMatched
without changing matching behavior. This is a prediction to compare against the
actual D4 diff, not a claim that those boundaries have been proved.

Normal tests: a matched pair adds several exercises, retrieves them in order,
and sees the same routine from both sessions; existing swipe and chat behavior
still passes. Bad-input tests: empty/null names, nonpositive sets/repetitions,
negative or nonfinite weights, malformed numeric console input, unmatched
writes/reads, and attempted mutation of returned routine data. Rejected actions
must leave the routine unchanged. Decide and document whether zero weight is
allowed before implementing validation.

The user confirmed pursuing shared workout routines and dropping distance-based
matchmaking and workout history for D4. Other original-scope stretch goals have
not been independently identified. No D4 feature code was added in this step.

## Attribution

The user confirmed Gowri wrote the five original profile/Queue files, despite
the specification's planned roles. The table distinguishes that report from
changes generated and commands executed by Codex in this session. The requesting
user's team identity and any human review/testing of the generated changes are
unconfirmed. Git commit author metadata is not treated as proof of authorship.

The desktop addition was also generated and tested by Codex at the user's request.
All paths below are under `src/spotter/`. This table includes every current
class/enum and test file. LinkedQueue's private Node class is included in its
file's row.

| File | Actual contribution supported by available information |
| --- | --- |
| UserProfile.java | Gowri: original implementation (user-confirmed). Codex: added optional goal/gym/time/bio fields and compatible constructor; tested validation. |
| UserProfileTest.java | Codex: generated and executed 2 profile constructor/detail validation tests. Human reviewer/tester unconfirmed. |
| LinkedQueue.java | Gowri: original implementation including Node (user-confirmed). Codex: inspected; no production changes; exercised through tests. |
| ProfileManager.java | Gowri: original implementation (user-confirmed). Codex: revised addProfile return value and checked callers/tests. |
| LinkedQueueTest.java | Gowri: original tests (user-confirmed). Codex: added two edge-case tests and executed suite. |
| ProfileManagerTest.java | Gowri: original tests (user-confirmed). Codex: added return-value assertions and executed suite. |
| SwipeAction.java | Codex: generated LEFT/RIGHT enum; exercised through workflow tests. Human reviewer/tester unconfirmed. |
| MainLogic.java | Codex: generated model orchestration; exercised through model and Controller tests. Human reviewer/tester unconfirmed. |
| MatchManager.java | Codex: generated shared swipe/mutual-like state; exercised through workflow tests. Human reviewer/tester unconfirmed. |
| ChatManager.java | Codex: generated matched-only messaging and copied conversation output; exercised through workflow tests. Human reviewer/tester unconfirmed. |
| Message.java | Codex: generated immutable sender/content/timestamp fields and content validation; used by messaging tests. Human reviewer/tester unconfirmed. |
| MainLogicTest.java | Codex: generated and executed 11 workflow tests. Human reviewer/tester unconfirmed. |
| SpotterView.java | Codex: generated minimal console formatting; exercised through integration tests/process runs. Human reviewer/tester unconfirmed. |
| SpotterController.java | Codex: generated command routing/parsing; corrected map null check after failing tests; executed integration checks. Human reviewer/tester unconfirmed. |
| SpotterApp.java | Codex: generated demo wiring; executed separate console processes. Human reviewer/tester unconfirmed. |
| SpotterSwingView.java | Codex: generated optional Swing card/chat View, rendered preview, and exercised button events headlessly. Human reviewer/tester unconfirmed. |
| SpotterSwingViewTest.java | Codex: generated and executed 3 desktop interaction tests, including creating a profile and matching/messaging with an existing account. Human reviewer/tester unconfirmed. |
| SpotterControllerTest.java | Codex: generated and executed 3 integration tests. Human reviewer/tester unconfirmed. |

Codex also updated `.classpath` and this README at the user's request. The
pre-existing `.project` resource-filter edit was left untouched; its contributor
is not established here. Team members should confirm substantive contributions
and human review before submitting; intended ownership is not actual attribution.

## GenAI usage note

The supplied specification reports ChatGPT assistance with MVC design, method
signatures, data/state, validation, testing, and scope clarification. That is
the document's account, not independently verified authorship of source files.

In this session, the user asked Codex to inspect the requirements/repository,
compare the implementation with the rubric, establish a test baseline, complete
the independent Queue/model checks, implement the missing core workflow, connect
a minimal console, and write an evidence-based README. Codex generated the new
files attributed above, revised ProfileManager and tests, configured JUnit 5,
and executed the reported compiler/test/process checks. Existing working Queue
and profile behavior was reused. Earlier checks used Eclipse's batch compiler;
Step 6 used javac with Java 17 and the cached JUnit runner.

The user supplied original-file attribution and confirmed stretch-goal choices;
remaining owners and human review/testing are unconfirmed. The proposed D4
impact prediction above was prepared with Codex assistance and has not been
validated by implementing the feature. No generated feature is presented as
human-authored work, and no unexecuted test is reported as passing.
