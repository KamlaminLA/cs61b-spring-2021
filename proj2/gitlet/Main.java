package gitlet;

import java.rmi.server.RemoteRef;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            Utils.existWithError("Please enter a command.");
        }
        // TODO: handle some failure case before process the args
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                Repository.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Incorrect operands.");
                Repository.stageForAddition(args[1]);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Please enter a commit message.");
                Repository.createCommit(args[1]);
                break;
            case "rm":
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Incorrect operands.");
                Repository.stageForRemoval(args[1]);
                break;
            case "log":
                Utils.checkInitialized();
                Utils.validateOperand(args, 1, "Incorrect operands.");
                Repository.getLogs();
                break;
            case "checkout":
                Utils.checkInitialized();
                Repository.checkout(args);
                break;
            case "global-log":
                Utils.checkInitialized();
                Repository.getAllCommits();
                break;
            case "find":
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Incorrect operands.");
                Repository.findCommit(args[1]);
                break;
            case "status":
                Utils.checkInitialized();
                Utils.validateOperand(args, 1, "Incorrect operands.");
                Repository.printStatus();
                break;
            case "branch":
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Incorrect operands");
                Repository.addBranch(args[1]);
                break;
            case "rm-branch":
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Incorrect operands");
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Incorrect operands");
                Repository.reset(args[1]);
                break;
            case "merge":
                Utils.checkInitialized();
                Utils.validateOperand(args, 2, "Incorrect operands");
                Repository.merge(args[1]);
                break;
            default:
                Utils.existWithError("No command with that name exists.");
        }
    }
}
