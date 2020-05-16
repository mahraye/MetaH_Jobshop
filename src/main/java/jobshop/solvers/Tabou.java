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

public class Tabou implements Solver {

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
        GloutonESTLRPT glouton = new GloutonESTLRPT();
        Result s = glouton.solve(instance,deadline);
        //s_iteration = meilleur solution pour l'itération
        Result s_iteration = s;
        int best = s.schedule.makespan();
        //Liste des éléments non autorisé ( déjà visité)
        int List_Taboo [][] = new int[instance.numJobs*instance.numMachines][instance.numJobs*instance.numMachines];
        //k permet de compter les itérations
        int i = 0;
        int maxIter=100;
        int dureeTabou=10;
        //tant que le nombre d'itération max n'est pas atteinte et que la deadline n'est pas atteinte
        while (i< maxIter && deadline - System.currentTimeMillis() > 1) {
            i++;
            //l'order qui correspond au meilleur schedule (s)
            ResourceOrder order = new ResourceOrder(s.schedule);
            //l'order qui correspond au meilleur schedule de l'itération (s_local)
            ResourceOrder order_iteration = new ResourceOrder(s_iteration.schedule);
            //la liste des Block du chemin critique
            List<Block> blocksList = blocksOfCriticalPath(order_iteration);
            //variables pour stocker les meilleurs résultats locaux
            Swap bestSwap = null;
            int best_iteration = -1;
            //on parcourt les blocks et les swap
            for (Block block : blocksList) {
                //la liste des Swap pour le Block
                List<Swap> swapList = neighbors(block);
                for (Swap swap : swapList) {
                    //avant de tester le swap, on vérifie que la solution n'est pas dans l'essemble tabou (déjà visité)
                    if (i > List_Taboo[swap.t1][swap.t2]) {
                        //on copie l'ordre de s et on applique le swap
                        ResourceOrder copy = order_iteration.copy();
                        swap.applyOn(copy);
                        int makespan = copy.toSchedule().makespan();
                        //si le swap retourne un meilleur résultat que le résultat local on actualise s_itération
                        if (best_iteration == -1 || makespan < best_iteration) {
                            bestSwap = swap;
                            best_iteration = makespan;
                            order_iteration = copy;
                            //si le swap est également meilleur que s, on actulise s
                            if (makespan < best) {
                                best = makespan;
                                order = copy;
                            }
                        }
                    }
                }
            }
            //si un swap est meilleur que la solution locale on l'ajoute à la structure
            if (bestSwap != null) {
                List_Taboo[bestSwap.t1][bestSwap.t2] = i + dureeTabou;
            }
            //on actualise s et s_iteration
            s_iteration = new Result(order_iteration.instance, order_iteration.toSchedule(), Result.ExitCause.Blocked);
            s = new Result(order.instance, order.toSchedule(), Result.ExitCause.Blocked);
        }
        //en fonction de si maxIter a été atteint ou si la deadline a été atteinte
        //on ne retourne pas la même raison de sortie
        if (i == maxIter) return s;
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