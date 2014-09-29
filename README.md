# MasterTap

This is a PoC Android app that can effectively clone PayPass-enabled MasterCard credit cards and emulate them for contactless transactions. It implements the [combined pre-play and downgrade attack](https://www.usenix.org/conference/woot13/workshop-program/presentation/roland) on EMV contactless by Roland and Langer (2013).

![](http://i.imgur.com/MlIVuOA.png)
![](http://i.imgur.com/wKuuZB5.png)

## Features

* Store multiple cards and swipe to the desired card to select it for a contactless transaction.
* Resume incomplete cloning.
* Import/export cards in JSON format.
* Lock/unlock application with a password.
* Sensitive data is stored encrypted using SQLCipher.

## Usage

* **Initial setup**: Accept the disclaimer and register a numeric password.
* **Card cloning**: Place the device over the card to clone it. If the device loses contact with the card, cloning will pick up where it left off when contact is re-established.
* **Card emulation**: Simply swipe to the desired card and place the device over the POS terminal to perform a contactless transaction.

## Limitations and Known Issues

* SQLCipher key is not salted.
* Only supports Android 4.4 (KitKat) and higher.
* Has not been tested thoroughly and is not ready for regular/stable usage.

## Licence

[MIT](http://www.tldrlegal.com/l/MIT)
