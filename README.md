BIGnav is based on Bayesian Experimental Design, using the criterion of mutual information from information theory, which is also known as information gain.

At each step, instead of simply executing user input in multiscale navigation, BIGnav searches for the view that maximizes the expected information gain from the user's subsequent input to effectively gain information and to reduce its uncertainty about the user's goal.

For more information, please refer to the paper: http://dl.acm.org/citation.cfm?id=3025524&CFID=758085361&CFTOKEN=11299043

To use BIGnav, you need 3 elements:

(1) \Theta represents the points of interest, P(\Theta = \theta) is the computer's prior knowledge about the user's interest - the probability of each one of the points being the intended target.

(2) X represents any system feedback, here the possible view sent to the user. At each time, the computer sends one particular view X = x to the user.

(3) Y represents any user input, here panning and zooming. At each time, the user sends one particular input Y = y to the computer.

To make BIGnav work, you also need 3 functions:

(1) The user behavior function P(Y = y | X = x, \Theta = \theta) - the computer needs to know what is the probability of the user giving certain command given what she wants and what she sees. This is defined in the function Pr_BGI(...) in the codes. One can define this as one wishes. It can also be user-independent.

(2) Update the computer's knowledge P(\Theta = \theta | X = x, Y = y) - after sending the view X = x to the user and receiving the user's input Y = y, the computer can update the probability of the objects being the intended target. This is defined in the function newupdate().

(3) At each step, BIGnav goes over all the possible views and locates the one that is maximally informative from the user's subsequent input. This is defined in the function BestWP().


To make the computation tractable, the user input Y is currently discretized into 8 panning directions and 1 zooming-in region. This is defined in the function direction(). And the view is discretized into tiles (200 × 150 pixels each), so BIGnav would search in this set instead of searching the whole space. In the future, we would like to reduce the size of the tiles and increase the number of panning directions to provide finer control.


Finally, the environment is enabled by ZVTM. You can find more information here: http://zvtm.sourceforge.net

Also, if you want to know more about information theory and bayesian experimental design, here are some references:

[1] Cover, T. M., & Thomas, J. A. (2012). Elements of information theory. John Wiley & Sons.

[2] Théorie de l'information et du codage (en français): http://perso.telecom-paristech.fr/~rioul/books/theorieinfocodage.html

[3] Chaloner, K., & Verdinelli, I. (1995). Bayesian experimental design: A review. Statistical Science, 273-304.


Cheers,
