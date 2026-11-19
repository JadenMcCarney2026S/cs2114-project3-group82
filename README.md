# Spotter - CS 2114 Project 3, Group 82

Spotter helps users find gym partners. The D4 prototype queues eligible profiles
in FIFO order, lets users like or pass, and unlocks chat and a shared workout
after a mutual match. The console and local Swing app show the D4 workflow.
The separate Wi-Fi and Android apps demonstrate account-based onboarding,
discovery, and chat.

## Running the model, tests, and console

Use JDK 17. The repository is an Eclipse Java project; `.classpath` declares
JUnit 5 for the desktop project. Android has its own Gradle build. Run commands from the repository
root. Terminal examples also require `rg` (ripgrep). Do not use old `bin/`
classes as evidence that the current source builds.

### Eclipse

1. Import the folder using File > Import > General > Existing Projects into
   Workspace. Use Eclipse with Java and JUnit 5 support and select JDK 17.
2. Select Project > Clean. Run `LinkedQueueTest`, `ProfileManagerTest`, and
   `MainLogicTest`, `WorkoutManagerTest`, and `SpotterAccountsTest` using Run As > JUnit Test for model-only checks.
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
  $(rg --files src/spotter -g '*.java' | rg -v '/(Spotter|ProfileView|MatchesView|ChatView)') \
  src/spotter/SpotterAccounts.java src/spotter/SpotterAccountsTest.java
java -jar "$JUNIT" execute --class-path "$MODEL_OUT" \
  --scan-class-path --fail-if-no-tests --details summary
```

This compilation excludes every View, Controller, launcher, and UI test file,
including the separately merged ProfileView, MatchesView, and ChatView.
The D4 verification run passed 44 independent model tests and 90 tests in the
full desktop and HTTPS suite. The console guided demo was also run through
completion with the shared-workout steps.

| D4 behavior | Where to verify |
| --- | --- |
| FIFO under interleaved inserts and removes, null inserts, and empty peek/remove | `LinkedQueueTest` |
| Shared workout ordering, both participants can read and add, unmatched and self access are denied | `WorkoutManagerTest`, `MainLogicTest` |
| Blank or null exercise name, zero or negative counts, negative or nonfinite weight, immutable snapshots | `WorkoutManagerTest`, `MainLogicTest` |
| Malformed command numbers and routine output through both Views | `SpotterControllerTest`, `SpotterSwingViewTest` in the full suite |
| Empty chat, unmatched chat, bounded history, and stale cursors | `MainLogicTest`, `ChatManagerTest` |
| Invalid credentials, duplicate usernames, underage profiles, invalid or oversized photos, and expired sessions | `SpotterAccountsTest` |

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

To run the full suite after the same compilation and resource copy:

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
| `createworkout <name>` | Create or replace your personal workout. |
| `addexercise <name>` | Add an exercise to your personal workout. |
| `workout` | View your personal workout. |
| `workout <id>` | View a workout shared with a mutual match. |
| `exercise <id> <sets> <reps> <weight> <name>` | Add an exercise to the shared workout. Enter zero weight for bodyweight exercises. |
| `user <id>` | Switch between existing demo sessions. This is not login. |
| `help` / `quit` | Show commands or exit. EOF also exits. |

For a presentation, launch the guided demo instead:

```sh
java -cp "$CONSOLE_OUT" spotter.SpotterApp --demo
```

Press Enter to run each explained step, or type `quit` to stop. The demo
uses real Controller commands and reciprocal swipes, not seeded matches. Dave
makes queue advancement visible; the guided flow also demonstrates a left swipe,
an empty queue, rejected messaging, shared conversations, the shared workout,
and invalid input. For the grading session, use the timed path under Next Steps.
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

#### Review the D4 console prototype

Start a fresh `SpotterApp` using the build command above. Enter each row's
commands one at a time and compare the result with the last column.

| Check | Commands | Expected result |
| --- | --- | --- |
| Queue and one-sided Like | `profile`, `right`, `matches` | Alice first sees Bob. After the Like, Carol is next and the match list is empty. |
| Mutual match | `user 2`, `right`, `matches`, `workout 1` | Bob matches Alice. Their new routine has no exercises. |
| Shared routine and order | `exercise 1 3 12 0 Push ups`, `exercise 1 3 8 25 Dumbbell press`, `user 1`, `workout 2` | Alice sees Bob's two exercises in the order entered, including 0 lb for bodyweight. |
| Invalid input | `exercise 2 0 8 25 Invalid`, `workout 2` | The error explains the input rules. Both valid exercises remain in the same order. |
| Access control | `user 3`, `workout 1`, `exercise 1 3 8 0 Push ups` | Carol sees no accessible exercises and her attempt to add one is rejected. |

Type `quit` to exit. Restart the app to review from the original sample
profiles. For the timed presentation, see [Three-minute demo](#three-minute-demo).

### Desktop card-and-chat demo

An optional Java Swing View keeps the same Queue, matching, and messaging
rules and routes buttons through SpotterController. No extra library is needed.
Compile production sources using the console instructions, then launch:

```sh
cp -R src/spotter/photos "$CONSOLE_OUT/spotter/"
java -cp "$CONSOLE_OUT" spotter.SpotterSwingView
```

In Eclipse, run `SpotterSwingView.java` as a Java Application. The app opens one
window for Alice and one for Bob. In **Discovery**, like Bob in Alice's
window and like Alice in Bob's window. Open **Messages**, select the resulting
match in both windows, then send a message from either window. The message
appears in both conversations automatically.
**Like** and **Pass** briefly cover the profile card with **LIKED!** or
**PASSED…**. The panel slides away to reveal the next profile. Add
`-Dspotter.reduceMotion=true` before `-cp` in the Swing launch command to
keep the feedback stationary.
Switching either window to Carol clears the open conversation. Both windows
share the same in-memory demo state within one Java process.
Open **Shared workout** to use the full-width routine panel. Choose a mutual
match, enter an exercise
name, sets, repetitions, and weight in pounds, then choose **Add exercise**.
Both participants can read the routine. Enter 0 lb for bodyweight exercises.
Pass and Like disable when a queue is exhausted. The dropdown is demo identity
switching, not authentication. The four fixed demo profiles use user-supplied sample photos: Alice and Carol
use the two female portraits; Bob and Dave use two male portraits. These are
fictional demo assignments, not claims about the people pictured. New profiles
without a photo use their initial.

SpotterApp.createSessions is shared by console and desktop launchers; each new
launch starts fresh in-memory state. The original console remains available.

### Run Spotter on an Android emulator

The Android app lives in `android/` and uses the same account, profile,
discovery, and chat server as the desktop app. Install Android Studio with the
Android 35 SDK, Build Tools 35.0.0, Platform-Tools, and Android Emulator.
Create an API 28 or newer virtual device and start it. The project uses
JDK 17 and its included Gradle Wrapper.

From the project folder with Docker running, execute:

```sh
sh scripts/spotter-android.sh
```

The launcher starts the Compose server, includes its public HTTPS certificate
in the debug build, forwards port 8443 with ADB, and installs and opens Spotter.
If more than one Android device is connected, set `ANDROID_SERIAL` to the
emulator serial shown by `adb devices`. For an existing local Spotter HTTPS
server using port 8443 and a certificate for `127.0.0.1`, pass its public
certificate as the only argument. The certificate is refreshed on each
launcher run and excluded from Git.

Choose **Sign up**, then add a profile photo. To test discovery and chat, use
two accounts on separate clients with the same workout interest.

### Run the Swing app with Docker

Install and start Docker Desktop or Docker Engine with Compose. The computer
displaying Swing also needs JDK 17. From the project folder on macOS or Linux,
run:

```sh
sh scripts/spotter-docker.sh
```

The launcher builds and starts the server, trusts its generated TLS
certificate for this client, and opens Swing with the server address filled
in. Choose **Sign up** or **Log in**. Run the same launcher in another terminal
to open a second Swing client against the same server.

By default, the Docker server is reachable only from this computer at
`https://127.0.0.1:8443/chat`. Accounts and photos stay in the
`spotter-data` Docker volume. Use `docker compose down` to stop the server
and keep the volume. Accounts from the manual server use a separate data file.

To accept Swing clients from another computer on the same Wi-Fi, start the
host with its Wi-Fi IPv4 address and export its public certificate. Replace the
example IP with the host's actual address:

```sh
export SPOTTER_LAN_IP=192.168.1.10
sh scripts/spotter-docker.sh
```

In another terminal on the host, run `docker compose cp server:/data/server.crt ./spotter-docker.crt`.
Share the public certificate with participants through a trusted channel.
Each participant runs the following from their project folder using JDK 17:

```sh
sh scripts/spotter-docker.sh https://192.168.1.10:8443/chat /path/to/spotter-docker.crt
```

Only the host needs Docker. Each client uses the public certificate for the
duration of its Swing session.

### Run the Wi-Fi server manually

Compile the Java 17 production sources using the console instructions above
on each computer. On the host, find its Wi-Fi IPv4 address. Generate a TLS
certificate for that address using JDK 17's `keytool`. Replace the example IP
and keep the private keystore outside the repository:

```sh
keytool -genkeypair -alias spotter-lan -keyalg RSA -keysize 2048 \
  -dname "CN=Spotter LAN" -ext "SAN=ip:192.168.1.10" -validity 90 \
  -storetype PKCS12 -keystore "$HOME/spotter-lan-host.p12"
keytool -exportcert -rfc -alias spotter-lan -keystore "$HOME/spotter-lan-host.p12" \
  -file "$HOME/spotter-lan-host.crt"
keytool -printcert -file "$HOME/spotter-lan-host.crt"
java -cp "$CONSOLE_OUT" spotter.SpotterLanServer 192.168.1.10 8080 \
  "$HOME/spotter-lan-host.p12"
```

The server prompts for the host keystore password. For Eclipse launches that
have no terminal prompt, set `SPOTTER_KEYSTORE_PASSWORD` in the local launch
environment. Keep the server running and share its HTTPS address and public
certificate (`.crt`) with participants. The server stores accounts and
profiles in `~/.spotter/accounts.dat` by default. A fourth server argument
selects a different path. Keep this file on the host since it contains private
account and profile data.

On every client computer, confirm that `keytool -printcert` reports the SHA-256
fingerprint the host provided through a trusted channel before importing the
certificate. Use a separate password for this client truststore, which holds
only the public certificate. Replace `CLIENT_STORE_PASSWORD` with that password:

```sh
keytool -printcert -file "$HOME/spotter-lan-host.crt"
keytool -importcert -alias spotter-lan -file "$HOME/spotter-lan-host.crt" \
  -storetype PKCS12 -keystore "$HOME/spotter-lan-trust.p12"
java -Djavax.net.ssl.trustStore="$HOME/spotter-lan-trust.p12" \
  -Djavax.net.ssl.trustStoreType=PKCS12 \
  -Djavax.net.ssl.trustStorePassword='CLIENT_STORE_PASSWORD' \
  -cp "$CONSOLE_OUT" spotter.SpotterLanView
```

Enter the HTTPS URL printed by the server and choose **Sign up** or **Log in**.
Usernames must have 3–32 letters, digits, or underscores and passwords must
have 12–128 characters. LAN discovery is for adults 18 and older. Add a
display name, age, workout interest, and JPEG or
PNG photo to complete your profile. The bio is optional. Returning users go
straight to discovery or finish their profile if they left it incomplete.

The LAN host starts with an empty roster. Create two accounts with the same
workout interest to try discovery. Like each other's profiles and select
the other person under
**Mutual match**, and send a message. The conversation refreshes about once a
second and retains the 100 most recent messages per pair. If a send is
unconfirmed, retry the same draft. The server recognizes duplicate requests
for the last 256 successful sends per account. If the host's IP
changes, generate and import a certificate for the new IP. If clients cannot
connect, check that Wi-Fi allows connections between devices and that the host
firewall allows inbound TCP on the chosen port. Closing the server clears
matches, messages, and login sessions. Accounts and photos persist.

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

### Earlier D3 verification

These counts record the D3 test run on Java 17.0.12. See the D4 model-only and full-suite commands above for the current prototype.

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

### D3 rubric evidence at the earlier checkpoint

| Criterion | Evidence | Remaining confirmation |
| --- | --- | --- |
| Model Works / Exceeds | LinkedQueueTest: 10 passing Queue tests; 33 model-only tests with no UI compiled. | Team can explain front/rear links and FIFO. |
| Thin View / Exceeds | Console and Swing actions call SpotterController, then MainLogic; model has no UI imports. | Local launch and Bob-to-Carol card/photo advancement manually checked. |
| Runs Without Crashing / Exceeds | Full workflow, null/left/duplicate swipes, exhausted queues, bad commands/IDs, rejected messages and unauthorized reads have passing tests. | Jaden completed manual mutual matching, messaging/replies, and Carol account-isolation checks (team-confirmed). |
| Reflection / Exceeds | Each implementation deviation below names its actual trigger or test failure. | Team factual review. |
| Next Steps / Exceeds | Shared-workout proposal named methods, unchanged classes, normal/bad-input tests, and confirmed owners. | The feature was still planned at this checkpoint. |
| Attribution + GenAI | The table records the original implementation, AI-assisted changes, and manual testing. | The D4 rows below record newer files and contributions. |

Bad-input evidence: LinkedQueueTest covers null and empty operations;
ProfileManagerTest covers self/duplicates/filtering/empty access;
UserProfileTest covers invalid construction and rejected edits preserving state;
MainLogicTest covers null swipes preserving queue/history, duplicate decisions,
empty queues/messages and unmatched conversation access;
SpotterControllerTest covers malformed commands/IDs and EOF. These tests prove
the named cases, not exhaustive input coverage or authenticated user security.

## Reflection

### How the design absorbed the D4 change

D3 proposed shared workout routines. D4 implements that feature on the console
and local Swing paths. A mutual match can add named exercises with sets,
repetitions, and weight. Either participant can read the same routine in
insertion order.

| D3 prediction | Actual D4 changes |
| --- | --- |
| Add a workout model and pair-keyed state; let MainLogic enforce access. | Added `Workout` and `WorkoutManager`. `MainLogic.addWorkoutExercise` and `getWorkout` delegate to the manager, which checks the existing mutual-match rule. |
| Route actions through the Controller and display snapshots. | `SpotterController` parses the console commands and routes both Views. `SpotterView` formats the results and `SpotterSwingView` accepts inputs and displays the selected match’s routine. |
| Share workout state when building demo sessions. | `SpotterApp` wires one manager into the demo sessions. Profile creation also needed wiring in `SpotterController` so later profiles join the same store. |
| Leave Queue, eligibility, profile, message, chat, and matching rules alone. | Adding workouts required no edits to `LinkedQueue`, `ProfileManager`, `UserProfile`, `Message`, `ChatManager`, or `MatchManager`. The Queue test file changed to demonstrate interleaving. Earlier standalone chat changes are separate from the workout change. |
| Verify normal, invalid, and unauthorized paths. | Added workout model tests and exercised the feature through MainLogic, Controller commands, and Swing. Zero weight represents bodyweight work; negative or nonfinite weights are rejected. |

The feature crosses the model/View boundary through the Controller. The View
knows which form inputs to collect and how to render a routine snapshot.
Authorization, validation, exercise order, and storage stay in the model.
For the next shared feature, making the shared demo session construction
available through one factory would reduce wiring in both the launcher and
the new-profile path.

The grading demo uses `SpotterApp` and the console `SpotterView`.
`SpotterView` holds input and output handles. It displays profiles, matches,
and workout snapshots supplied by the Controller during each call. The model
owns the candidate queue and shared workout. The Swing and LAN clients have
separate presentation state.

The workout changes can be checked independently of the earlier live-chat work:

```sh
git show --stat 858c6c8
git diff --name-only 432124f..858c6c8 -- \
  src/spotter/LinkedQueue.java src/spotter/ProfileManager.java \
  src/spotter/UserProfile.java src/spotter/Message.java \
  src/spotter/ChatManager.java src/spotter/MatchManager.java
```

The second command produces no paths. Across the wider project history,
`ChatManager` changed for earlier live-chat and LAN history support. The
shared workout reuses the existing match check without changing it.

### What remains

Workouts are in memory, can be appended and read, and are available in the
console and local Swing demo. These sessions explicitly share a single
`WorkoutManager`. Editing, deleting, persistent routines, and
Wi-Fi/Android workout screens are outside this implementation. Account and
photo persistence belong to the separate Wi-Fi app. Its matching state and
chat reset when the server restarts. The Android client was built and its
network protocol tested separately; an emulator UI run remains unverified.

## Next Steps

| Order | Remaining work for the final build | Proposed owner by existing team role |
| --- | --- | --- |
| 1 | Rehearse the demo on the lab machine and check the clean JDK 17 build and test instructions. | Jaden (integration), Varooni (View) |
| 2 | Review exercise validation and access with the original Queue and matching tests, and explain the model in the grading session. | David (workout model), Gowri (matching and access) |
| 3 | Verify each file-level contribution with the team and update the rows with completed human review or tests. | Each contributor confirms their entries; Jaden coordinates. |

For the final build, commit to the shared workout and local console/Swing
demo. Drop the previously identified distance-based matching and workout
history stretch goals. Defer Wi-Fi/Android workout support and routine
persistence until the graded desktop flow is complete.

### Three-minute demo

| Time | Show | Console actions |
| --- | --- | --- |
| 0:00–0:35 | Alice sees Bob, likes him, and advances FIFO. A one-sided like gives no match. | `profile`, `right`, `matches` |
| 0:35–1:05 | Bob likes Alice and a mutual match opens. | `user 2`, `right`, `matches`, `workout 1` |
| 1:05–1:55 | Bob creates two exercises. Alice reads the same routine in order. | `exercise 1 3 12 0 Push ups`, `exercise 1 3 8 25 Dumbbell press`, `user 1`, `workout 2` |
| 1:55–2:30 | Invalid sets are rejected, and Carol cannot read or write Alice’s routine. | `exercise 2 0 8 25 Invalid`, `user 3`, `workout 1`, `exercise 1 3 8 0 Push ups` |
| 2:30–3:00 | Show the passing interleaved FIFO and workout model tests and explain the shared store. | Open `LinkedQueueTest` and `WorkoutManagerTest`. |

Before the timer starts, build and run the model-only tests, then leave their
passing summary open. Launch a fresh `SpotterApp` and keep the commands above
beside it. During the demo, explain that removing Alice's front entry leaves
Carol at the front and that both directions of a matched pair resolve to the
same workout key. Point to the invalid-set error and Carol's empty read before
showing the test summary. Time one spoken run on the submission machine.

## Attribution

The table records original implementation, Codex-assisted changes, and manual
testing. Codex generated/revised code and executed automated checks under team
direction. Team members confirmed their attribution entries and D4 roles;
this confirmation is separate from a detailed source-code review. AI-assisted
work is labeled explicitly. Newer rows marked Codex-assisted describe work
carried out under David's direction; they do not claim review or testing by
other team members.

Files are under `src/spotter/` unless a path is shown. The table lists
top-level Java classes, enums, and test files. LinkedQueue's private Node class is included in its
file's row.

| File | Actual contribution supported by available information |
| --- | --- |
| UserProfile.java | Gowri: original implementation (team-confirmed). David (Codex-assisted): added optional goal/gym/time/bio fields and compatible constructor; tested validation. |
| UserProfileTest.java | David (Codex-assisted): generated and executed 5 constructor, edit-validation, and defensive-copy tests. |
| LinkedQueue.java | Gowri: original implementation including Node (team-confirmed). David (Codex-assisted): inspected; no production changes; directed inspection and automated test execution. |
| ProfileManager.java | Gowri: original implementation (team-confirmed). David (Codex-assisted): revised addProfile return value and checked callers/tests. |
| LinkedQueueTest.java | Gowri: original tests (team-confirmed). David (Codex-assisted): added edge cases and interleaved FIFO coverage. |
| ProfileManagerTest.java | Gowri: original tests (team-confirmed). David (Codex-assisted): added return-value assertions and executed suite. |
| SwipeAction.java | David (Codex-assisted): generated LEFT/RIGHT enum; exercised through workflow tests. |
| MainLogic.java | David (Codex-assisted): generated model orchestration; exercised through model and Controller tests. Codex assisted with D4 shared-workout delegation. |
| MatchManager.java | David (Codex-assisted): generated shared swipe/mutual-like state; exercised through workflow tests. |
| ChatManager.java | David (Codex-assisted): generated matched-only messaging and copied conversation output; exercised through workflow tests. |
| Message.java | David (Codex-assisted): generated immutable sender/content/timestamp fields and content validation; used by messaging tests. |
| MainLogicTest.java | David (Codex-assisted): generated and executed 11 workflow tests. Codex assisted with shared-workout model coverage. |
| SpotterView.java | David (Codex-assisted): implemented console View and compatibility methods for Varooni's formatter/input APIs; exercised integration tests/process runs. Codex assisted with workout output and command documentation. |
| SpotterController.java | David (Codex-assisted): generated command routing/parsing; corrected map null check after failing tests; executed integration checks. Jaden: manually tested mutual matching, messaging/replies, and account isolation (team-confirmed). Codex assisted with workout command routing, validation, and shared manager wiring. |
| SpotterApp.java | David (Codex-assisted): generated demo wiring; executed separate console processes. Codex assisted with wiring the shared workout manager into the demo. |
| SpotterSwingView.java | David (Codex-assisted): directed the card and chat work, shared-workout screen, tab layout, profile redesign, swipe transitions, and sender-name display. Codex implemented the changes and checked rendering. David manually confirmed the earlier local launch and Bob-to-Carol photo advancement. Jaden manually tested match selection, messaging and replies, account switching, and Carol's conversation isolation (team-confirmed). |
| SpotterSwingViewTest.java | David (Codex-assisted): directed coverage for profile creation, matching and chat, shared workouts, tab layout, swipe feedback, and sender names. Codex wrote or revised the tests and ran the automated checks. |
| SpotterControllerTest.java | David (Codex-assisted): generated and executed 3 integration tests. Codex assisted with the workout command integration test. |
| ProfileView.java | Varooni: authored (team-confirmed). David (Codex-assisted): integrated existing formatter API. |
| MatchesView.java | Varooni: authored (team-confirmed). David (Codex-assisted): integrated existing formatter API. |
| ChatView.java | Varooni: authored (team-confirmed). David (Codex-assisted): integrated existing formatter API. |
| ProfileViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): enabled Jupiter annotations and executed 4 tests. |
| MatchesViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): enabled Jupiter annotations and executed 4 tests. |
| ChatViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): enabled Jupiter annotations and executed 5 tests. |
| SpotterViewTest.java | Varooni: authored (team-confirmed). David (Codex-assisted): adapted console assertions, enabled Jupiter annotations, and executed 12 tests. |
| Workout.java | Codex-assisted implementation of the exercise value and ordered routine. |
| WorkoutManager.java | Codex-assisted implementation of shared, match-authorized workout storage. |
| WorkoutManagerTest.java | Codex-assisted model tests for routine ordering, validation, and access. |
| SpotterAccountHttp.java | Codex-assisted account and profile HTTPS request handling. |
| SpotterAccounts.java | Codex-assisted credential, profile, session, and host storage implementation. |
| SpotterAccountsTest.java | Codex-assisted account and profile model tests, including credential and photo size boundaries. |
| ChatManagerTest.java | Codex-assisted model test for bounded chat history and cursor handling. |
| SpotterLanClient.java | Codex-assisted desktop HTTPS client implementation. |
| SpotterLanOnboarding.java | Codex-assisted Swing login, profile setup, and photo selection UI. |
| SpotterLanOnboardingTest.java | Codex-assisted photo preview and validation tests. |
| SpotterLanServer.java | Codex-assisted LAN server implementation and account/model integration. |
| SpotterLanStyle.java | Codex-assisted Swing styling and error dialog components. |
| SpotterLanStyleTest.java | Codex-assisted alert and button tests. |
| SpotterLanTest.java | Codex-assisted HTTPS onboarding and chat integration tests. |
| SpotterLanTls.java | Codex-assisted host TLS keystore loader. |
| SpotterLanView.java | Codex-assisted LAN discovery and chat UI. |
| SpotterLanViewTest.java | Codex-assisted LAN conversation draft tests. |
| android/app/src/main/java/spotter/mobile/MainActivity.java | Codex-assisted Android onboarding, discovery, and chat UI. |
| android/app/src/main/java/spotter/mobile/SpotterApi.java | Codex-assisted Android HTTPS client; verified against the local server on the JVM. |

The .classpath and README updates were also Codex-assisted. A pre-existing
.project resource-filter edit was preserved. Planned responsibilities are
separate from completed contributions.

## GenAI usage note

### November 19, 2026 session — David Moon

David used Codex to evaluate Spotter against the D4 rubric, prioritize work,
direct implementation and UI revisions, and prepare the console review and
presentation. Codex drafted and revised code and tests, built and ran the
prototype, checked Swing renders, and committed and pushed the stages David
authorized. Earlier human contributions and manual checks appear in the
attribution table above.

The session started at D3 commit `6c8df24`. Commit `b77c2de` was the last
pushed checkpoint before the final Swing edits.

| Area | Before the session | Changes made with Codex |
| --- | --- | --- |
| D4 and model tests | Shared workouts were a proposal. Queue tests covered basic FIFO and reusing a drained queue. | Implemented ordered, match-only workouts in the console and local Swing. Added interleaved FIFO, workout access and validation, and controller tests. Updated the Reflection, review guide, and three-minute demo. |
| Accounts and chat | The local demo switched between identities and reopened stored chats. | Added live desktop chat, HTTPS Wi-Fi chat, account and photo onboarding, Docker startup, and an Android client for the same server. |
| Swing presentation | Discovery and chat shared a window layout. By `b77c2de`, Discovery, Messages, and Shared workout had separate tabs. | Later edits added larger profile photos and bios, full-card **LIKED!** and **PASSED…** transitions that block accidental extra swipes, and sender names in Messages. |

Codex observed 44 independent model tests passing and 90 tests passing in
the full suite after the swipe transitions. After the sender-name change,
all 22 Swing and MainLogic tests passed, and the final full-suite run passed
all 90 tests. Codex exercised the console review
steps, rendered Swing previews, packaged the Android APK, and checked the
mobile HTTPS protocol. Android emulator and Docker container runs were
unverified on this Mac.

### Earlier GenAI-assisted work

The supplied specification reports ChatGPT assistance with MVC design, method
signatures, data/state, validation, testing, and scope clarification. That is
the document's account, not independently verified authorship of source files.

For this deliverable, the team used Codex to inspect the requirements/repository,
compare the implementation with the rubric, establish a test baseline, complete
the independent Queue/model checks, implement the missing core workflow, connect
a minimal console, and write an evidence-based README. Codex generated the new
files attributed above, revised ProfileManager and tests, configured JUnit 5,
and executed the reported compiler/test/process checks. Existing working Queue
and profile behavior was reused. Earlier checks used Eclipse's batch compiler;
Step 6 used javac with Java 17 and the cached JUnit runner.

The team confirmed attribution, stretch-goal choices, and D4 owners. Manual GUI
checks are reported separately from automated results; detailed human source
review remains unconfirmed. The D4 impact prediction was prepared with Codex assistance. Codex helped
implement and test the workout change and compare the resulting changes with
that prediction. The latest rows record this assistance; the team should
confirm any additional human contributions before submission. Generated
code is identified as AI-assisted, and unexecuted tests are not reported as
passing.

Carol now shares Running as well as Swimming, so Alice sees Bob, Carol, then
Dave in FIFO order. This changes demo data only; shared-interest filtering is
still enforced. Passing or liking Bob advances to Carol's photo.
