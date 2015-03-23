![Flexion Logo](/images/flexion-logo.png?raw=true)

Flexion SDK Sample App - "Fun Flowers"
=======

Introduction
---------------
This is an example app using the Flexion billing SDK. For more information on the Flexion SDK, see **[flexionmobile.com](http://flexionmobile.com)**


This app is a simple game where the player can buy seeds and use it to 'grow'
randomly generated flowers. The player starts the game with a set amount of seeds. 
When the player grows a new flower, they consume a seed. If the player runs 
out of seeds, they can buy more using an in-app purchase.

The user can also purchase a "premium upgrade" that unlocks a special theme
for the app.

The user can also purchase a subscription ("magical water") which will 
make the flowers they grow larger. 

The app uses a local simulated Flexion billing server, so it works as a stand-alone application. 

<img src="/images/screenshot-0.png" align="left" height="620" width="349" hspace="5" vspace="20">
<img src="/images/screenshot-1.png" align="left" height="620" width="349" hspace="5" vspace="20">


---------------
Item consumption mechanics
---------------

It's important to note the consumption mechanics for each item:

PREMIUM THEME: the item is purchased and NEVER consumed. So, after the original
purchase, the player will always own that item. The application knows to
display the special picture because it queries whether the premium "item" is
owned or not.

MAGICAL WATER: this is a subscription, and subscriptions can't be consumed.

SEEDS: when seeds are purchased, the "seeds" item is then owned. We
consume it when we apply that item's effects to our app's world, which to
us means giving the player a fixed number of seeds. This happens immediately
after purchase! It's at this point (and not when the user drives) that the
"seeds" item is CONSUMED. Consumption should always happen when your game
world was safely updated to apply the effect of the purchase. So, in an
example scenario:

+ BEFORE:      the player has 5 seeds
+ ON PURCHASE: the player has 5 seeds, "seeds" item is owned
+ IMMEDIATELY: the player has 25 seeds, "seeds" item is consumed
+ AFTER:       the player has 25 seeds, "seeds" item NOT owned any more

Another important point to notice is that it may so happen that
the application crashed (or anything else happened) after the user
purchased the "seeds" item, but before it was consumed. That's why,
on startup, we check if we own the "seeds" item, and, if so,
we have to apply its effects to our world and consume it. This
is also very important!


Credits
---------------

This app is based on TrivalDrive by Bruno Oliveira (Google). TrivialDrive is licensed under the [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0). Modified to Fun Flowers by Jonathan Coe (Flexion Mobile). 