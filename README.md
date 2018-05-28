# NGO Secure - Blockchain build on R3 Corda Platform

This NGO Secure Blockchain app comprises a demo of an NGO Transaction that can be issued and settled confidentially. 

The app includes:

* NGO Transaction flows for
	-> Issuance
	-> Self issuance
	-> Settlement

* Following are the features available in the NGO Secure application
	1. NGO Transaction issuance between parties - An active transaction gets created in the system.
	2. NGO Transaction settlement - X amount of NGO Coins settled in a given active transaction.
	3. NGO Transaction self issuance - NGO Org can add NGO Coins to the coin vault.
	4. NGO Transaction ledger - Lists all transactions the given node is involved in (either as donor or organization)
	5. NGO Insights - This is the NGO Secure machine learning engine. The insights (if any) received from the NGO ML engine is displayed on the node UI. NGO Insights feature is available only for the parties colloborating on the block chain and this is not applicable for the notary.

# Instructions for setting up te NGO Secure application with R3 Corda Demo bench
1. Create the following folder repositories in the C Driver
	a. C:\BlockChain
	b. C:\BlockChain\Output
2.  git clone https://github.com/ngosecure/ngosecure_blockchain.git to the "C:\BlockChain" directory
3.  cd ngosecure_blockchain
4.  gradlew deployNodes
5. 	Download the R3 Cords demo bench for Windows - https://www.corda.net/downloads/
6.  Add the NGO Secure java-source-0.1.jar from the below build location
		C:\BlockChain\ngosecure_blockchain\java-source\build\libs
7.  Following #6 for all Nodes to the NGO Secure network and start the nodes along with the Notary
8.  Launch the web application by clicking on the "Launch Web Server" button.
9.  Click on the link "ngosecure" in the landing page and it will redirect you to the respective NGO Node's  
	homepage.

### NGO Secure ML Setup ###

For NGO Secure ML setup follow the steps in the ML-Readme.txt in the NGO Secure ML Git repository	
git clone https://github.com/ngosecure/ngosecure_ml.git to the "C:\BlockChain" directory
	