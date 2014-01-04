PaperWalletGen
==============
This is a very simple Bitcoin paper wallet generator.  Use it at your own risk.  You can [read more about paper wallets
in the Bitcoin wiki](https://en.bitcoin.it/wiki/Paper_wallet).

How to use
----------
You can run it using maven, like this:

<pre>mvn clean install exec:java</pre>

...or just run the App class from inside your IDE.  You will be presented with a small menu:

<pre>
1. Create paper wallet
2. Restore wallet from shared secrets
</pre>

Choose 1, and two PDF files will be generated, one named `paperwallet.pdf`, which is the actual paper wallet with the
address and the private key.  The second document, `secrets.pdf`, contains a backup of the wallet divided in four parts
(secrets).  Keep two of the part yourself, and give the rest to two people you trust.

Choose 2, to restore your wallet.  You only need three of the four parts.  Scan the QR codes to extract the
text string inside, and paste each of them in the console.  When done, the two PDF files described above will
be created.