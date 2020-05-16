package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1 = 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            Task temp = order.tasksByMachine[this.machine][this.t1];
            order.tasksByMachine[this.machine][this.t1] = order.tasksByMachine[this.machine][this.t2];
            order.tasksByMachine[this.machine][this.t2] = temp;
        }
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        // Solution initiale qui part de glouton LRPT
        GloutonESTLRPT glouton = new GloutonESTLRPT();
        Result s = glouton.solve(instance,deadline);
        int best = s.schedule.makespan();
        //tant que la deadline n'est pas atteinte
        while (deadline - System.currentTimeMillis() > 1) {
            // boolean permettant de savoir si une meilleur solution est trouvé (par default non trouvé)
            boolean Notfound = true;
            //init du meilleur rodrder actuel
            ResourceOrder order = new ResourceOrder(s.schedule);
            //instancier des blocks
            List<Block> blocks = blocksOfCriticalPath(order);
            //parcours des blocks et des swaps
            for (Block block : blocks) {
                List<Swap> swapList = neighbors(block);
                for (Swap swap : swapList) {
                    // on applique les swap
                    ResourceOrder copy = order.copy();
                    swap.applyOn(copy);
                    //calcul du new makespan après application des swap
                    int newM = copy.toSchedule().makespan();
                    //Si le nouveau makespan (après application du swap) est meilleur, actualisation de la solution initiale
                    if (newM < best) {
                        if (Notfound) Notfound = false;
                        order = copy;
                        best = newM;
                    }
                }
            }
            //Si on toujours pas trouvé de meilleur solution alors on rend la solution initiale
            if (!Notfound) s = new Result(order.instance, order.toSchedule(), Result.ExitCause.Blocked);
            else return s;
        }
        return new Result(s.instance, s.schedule, Result.ExitCause.Timeout);
    }

    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Task> criticalPath = order.toSchedule().criticalPath();
        List<Block> blocksList = new ArrayList<>();
        Task t = criticalPath.get(0);
        int machine = order.instance.machine(criticalPath.get(0));
        int firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(t);
        int lastTask = firstTask;
        for (int i = 1; i < criticalPath.size(); i++) {
            t = criticalPath.get(i);
            if (machine == order.instance.machine(t)) {
                lastTask++;
            } else {
                if (firstTask != lastTask) {
                    blocksList.add(new Block(machine, firstTask, lastTask));
                }
                machine = order.instance.machine(t);
                firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(t);
                lastTask = firstTask;
            }
        }
        return blocksList;

    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {

        List<Swap> swaps = new ArrayList<Swap>();
        int diff = block.lastTask - block.firstTask;

        if (diff >= 2) {
            swaps.add(new Swap(block.machine,block.firstTask,block.firstTask+1));
            swaps.add(new Swap(block.machine,block.lastTask-1,block.lastTask));
        }
        else {
            swaps.add(new Swap(block.machine,block.firstTask,block.lastTask));
        }

        return swaps;
    }

}