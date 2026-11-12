# cs2114-project3-group82
Gym partner finder

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

