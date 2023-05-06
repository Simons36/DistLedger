# Report

## Our Architecture

Our implementation of the gossip architecture is similar to the architecture presented in the book/PowerPoints, but has some key diferences:

- We don't have the executed operation table; to know if an operation has already been executed, we simply verify the atribute stable (if it is false, operation hasn't been executed, otherwise it has been executed).
- We don't store the replica timestamps from the other replicas
- In balance command, if the replica verifies the condition prevTS <= valueTS, if the request has asked for a balance of an account that does not exist, the replica will return error to the user. However, in updates (createAccount and transferTo) the replica always responds ok and never reports errors to users.

## How to use gossip command

Because our implementation supports three servers, our gossip works as follows:

> gossip <server_origin> <server_target1> .. <server_targetN>
