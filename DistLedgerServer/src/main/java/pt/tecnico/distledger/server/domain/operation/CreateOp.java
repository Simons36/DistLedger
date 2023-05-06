package pt.tecnico.distledger.server.domain.operation;

import java.util.List;

public class CreateOp extends Operation {

    public CreateOp(String account, int[] opTS, List<Integer> prevTS, List<String> serversNotGossiped) {
        super(account, opTS, prevTS, serversNotGossiped);
    }

    public CreateOp(String account, List<Integer> opTS, List<Integer> prevTS, List<String> serversNotGossiped) {
        super(account, opTS, prevTS, serversNotGossiped);
    }


}
