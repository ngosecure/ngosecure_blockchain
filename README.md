# NGO Secure - Blockchain Corda App

This NGO Secure Blockchain app comprises a demo of an NGO Transaction that can be issued, transfered and settled confidentially. 

The app includes:

* An transaction state definition that records an amount of any currency payable from one party to another. The transaction state
* A contract that facilitates the verification of issuance, transfer (from one lender to another) and settlement of transactions
* Three sets of flows for issuing, transferring and settling transactions. They work with both confidential and non-confidential transactions

The NGO Secure app allows you to issue, transfer and settle (with NGOCoin) transactions. It also 
comes with an API and webpage that allows you to do all of the aforementioned things.

# Instructions for setting up

1. `git clone https://github.com/ngosecure/ngosecure_blockchain.git`
2. `cd ngosecure_blockchain`
3. `/gradlew deployNodes` - building may take upto a minute (it's much quicker if you already have the Corda binaries)./r  
4. `cd java-source/build/nodes`
5. `/runnodes`

At this point you will have notary/network map node running as well as three other nodes and their corresponding webservers. There should be 7 console windows in total. One for the networkmap/notary and two for each of the three nodes. The nodes take about 20-30 seconds to finish booting up.

NOTE: That the transaction and corda-finance NGO Secure apps will automatically be installed for each node.

# Using the NGO Secure app via the web front-end

Following are the various nodes
1. http://localhost:10007
2. http://localhost:10011
3. http://localhost:10015
4. http://localhost:10019
5. http://localhost:10004 (Notary)

You'll see a basic page, listing all the API end-points and static web content. Click on the "ngosecure" link under "static web content". The dashboard shows you a number of things:

1. All issued transactions to date
2. A button to issue a new transaction
3. A button to self issue NGOCoin (used to settle transactions)
4. A refresh button
5. Ledger transactions

## Submit a donation

## Self issue some NGOCoin

## Settling an transaction

## Transaction Ledger
