# Android
## _CloudCoin Client using Android_

## Latest Version
Version 2.0.2

This Cloudcoin Wakllet implements CloudCoin 2.0 protocol using TCP Steam for the various operations except Echo.

## Features
- Echo
- Import PNG or binary files
- Export and share PNG files
- View and clear transaction history
- Downloading ID coins for usage during encryption
- Configurable health check on startup
- File system based checking for coins

## Todo
- Implement Encryption using a custom IV vector
- Work on scoped storage because future version of Android wont support public storage (some work done in scoped_storage branch)

## Important Classes
The main clases of interest are as follows -
**Core Classes**
- ```com.cloudcoin2.wallet.Utils.RAIDA.java``` - main CloudCoin 2.0 Protocol implementation class
- ```com.cloudcoin2.wallet.Utils.IDCoinGenerator``` - class for generating ID coins by calling the relevant RAIDA Service
- ```com.cloudcoin2.wallet.Utils.PngImage``` - class used for manipulating PNG chunks for reading and generating PNG Cloudcoins

**UI Structure**   
The UI consists of 2 Activities, ```HomeActivity``` and ```SplashActivity```. The ```SplashActivity``` only contains the initial Splash screen, whereas ```HomeActivity``` contains the main app.

```HomeActivity``` contains ```bottomNavigationView``` for the bottom menu tab to go to the various section, which replaces the top fragment within the activity between the following -

- ```com.cloudcoin2.wallet.home.HomeFragment``` - contains the home screen, with echo, health check, and display of the balance after counting coins using RAIDA class
- ```com.cloudcoin2.wallet.deposit.DepositFragment``` - contains the deposit screen, with Powning functionality using RAIDA/PngImage class
- ```com.cloudcoin2.wallet.withdraw.WithdrawFragment``` - contains the withdraw screen, with withdraw and export functionality using RAIDA and PngImage class
- ```com.cloudcoin2.wallet.transaction.TransactionFragment``` - contains the transaction screen showing the transaction history and also the clear history, only the history is maintained using Room DB.Core functionality is not dependent on this, this is just for informational purpose and tracking.
- ```com.cloudcoin2.wallet.settings``` - contains the settings screen allowing to modify the various settings of the app

**Debugging**
We are using Android's logcat for printing verbose output during the various processes, and in a production version of the app, the logcat output is redirected to a text file under the CloudCoin Logs folder. The relevant part of the code is in ```com.cloudcoin2.wallet.Utils.CouldcoinApplication``` class file ```onCreate()``` method.

Once the application becomes stable, may be a good idea to reduce the amount of log output, since it is quite verbose at the moment.

**Unit Testing**

**All core functionalities are unit tested and working as of 16th September 2022**
Unit tests are written in ```app/src/test/java``` folder. For the unit tests to work, especially for coin related tests, dummy coins needs to be placed in ``app/src/test/assets`` folder and their start/end serial numbers noted in the test classes, because all the core tests run in `CLI` mode, without an interface.