Building Fun Flowers
=====================

Overview
------
Fun Flowers is currently developed and built using Eclipse: [https://eclipse.org](https://eclipse.org)


Setting up a Development Environment
------

1) Make sure you have a Java JDK installed. The Android team recommends the Oracle JDK is over the Open JDK.

2) Download the Android SDK: [https://developer.android.com/sdk/index.html#download](https://developer.android.com/sdk/index.html#download)

3) Download and install Eclipse: [https://www.eclipse.org/downloads](https://www.eclipse.org/downloads)

4) Install the Android ADT plugin: [http://developer.android.com/sdk/installing/installing-adt.html](http://developer.android.com/sdk/installing/installing-adt.html)

5) Run the Android SDK manager through Eclipse and download any updates available

6) If it is not already installed, install [git](http://git-scm.com)

7) Clone the Fun Flowers Github repository to your system: git clone https://github.com/flexion-mobile/flexion-sdk-sample-app.git

8) Import Fun Flowers into Eclipse using the menu option File...Import...Import existing Android code

9) If you are on 64 bit Linux you may encounter build errors at this stage. You can try to fix these by installing some extra packages: sudo apt-get install lib32stdc++6 lib32z1 lib32ncurses5 lib32bz2-1.0

10) Fun Flowers should now be ready to be built! In Eclipse, go to "Project....Build project"

12) The file "FunFlowers-FlexionSDK.apk" should now be created in the /bin folder of the Eclipse project. You should be able to run this file on any Android device using Android API level 8 or later. 