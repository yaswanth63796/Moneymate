MoneyMate: Smart Finance & Payment Manager (Console Edition)
Overview

MoneyMate is a console-based Java application developed to simplify personal finance management.
The system allows users to register, log in securely, and manage their accounts with features such as deposits, withdrawals, transfers, transaction history, reminders, budget tracking, donations, and grocery expense management.

This project applies object-oriented programming principles, Java collections, exception handling, multithreading, and AES encryption to ensure both functionality and security. Future versions aim to integrate a database (MySQL/MongoDB) and a graphical interface using JavaFX.

Features
User Management

New users can register, with account numbers generated automatically.

Passwords are secured using AES encryption.

Multiple users are supported through an in-memory HashMap.

Account and Transactions

Users can deposit, withdraw, and transfer money between accounts.

A complete transaction history is maintained using ArrayList<Transaction>.

Custom exceptions (e.g., InsufficientBalanceException) handle invalid operations.

Smart Reminder System

Users can add bill reminders, including type, amount, and due date.

A background thread (using TimerTask) checks deadlines and alerts users.

Budget and Expense Tracking

Monthly salary and budget can be set by the user.

Expenses are tracked and the system warns when 80% of the budget is reached.

A monthly financial report is generated for better money management.

Donations to Charity Trusts

Users can donate to registered charity trusts.

All donations are recorded in the transaction history.

Provides transparency by maintaining a clear donation record.

Grocery Item Management

Users can add, view, and manage grocery items with price and quantity.

Tracks total grocery expenses and integrates them with the budget.

Maintains a personal grocery list using ArrayList<GroceryItem>.

Technical Highlights

Strong application of Object-Oriented Programming concepts (Encapsulation, Inheritance, Polymorphism).

Extensive use of Java Collections such as ArrayList, HashMap, and HashSet.

Implementation of custom exceptions for better error handling.

Multithreading using ScheduledExecutorService for automated reminder
