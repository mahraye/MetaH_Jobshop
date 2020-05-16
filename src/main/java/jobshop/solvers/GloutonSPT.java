package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import java.util.*;
public class GloutonSPT implements Solver {

    public Result solve(Instance instance, long deadline) {
        HashSet<Task> HashTasksRealisable = new HashSet<Task>();
        ResourceOrder RorderSolution = new ResourceOrder(instance);

        //initialisation (première tâche de chaque job dans ensemble réalisable
        for(int job = 0 ; job<(instance.numJobs) ; job++) {
            Task t = new Task(job,0);
            HashTasksRealisable.add(t);
        }
        // tant qu'il y'a des élément dans l'ensemble réalisable
        while(!HashTasksRealisable.isEmpty()){
            Task taskMin = null;
            //parcours des taches
            for(Task t : HashTasksRealisable) {
                //première itération
                if(taskMin == null){
                    taskMin=t;
                }
                // Priorité SPT (prendre la meilleur tâche)
                if (instance.duration(t) <instance.duration(taskMin)) {
                    //mise à jour en cas de meilleur solution
                    taskMin=t;
                }
            }
            //affectation de la tâche
            RorderSolution.addTask(instance.machine(taskMin),taskMin.job, taskMin.task);
            //On l'a supprimer de l'ensemble des tâche réalisable
            HashTasksRealisable.remove(taskMin);
            if(instance.numTasks-1>taskMin.task){
                //prochaine task réalisable est celle suivant la précédente
                HashTasksRealisable.add(new Task(taskMin.job,(taskMin.task +1)));
            }
        }


        return new Result(instance, RorderSolution.toSchedule(), Result.ExitCause.Blocked);
    }
}
