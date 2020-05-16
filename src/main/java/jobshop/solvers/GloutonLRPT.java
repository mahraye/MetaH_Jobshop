package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import java.util.*;
public class GloutonLRPT implements Solver {

    public Result solve(Instance instance, long deadline) {
        HashSet<Task> HashTasksRealisable = new HashSet<Task>();
        //instantiation des tableaux necessaire au calcul du temps EST
        int JobDuration[] = new int [instance.numJobs];
        int JobDurationDone[] = new int [instance.numJobs];
        HashSet<Task> HashTasksDone = new HashSet<Task>();
        ResourceOrder RorderSolution = new ResourceOrder(instance);

        //initialisation (première tâche de chaque job dans ensemble réalisable
        for(int job = 0 ; job<(instance.numJobs) ; job++) {
            Task t = new Task(job,0);
            HashTasksRealisable.add(t);
        }
        //initialisation tableau de durée
        for(int job=0; job<instance.numJobs; job++) {
            for (int task = 0; task < instance.numTasks; task++) {
                JobDuration[job]+=instance.duration(job,task);
            }
        }
        // tant qu'il y'a des élément dans l'ensemble réalisable
        while(!HashTasksRealisable.isEmpty()){
            Task taskMax = null;
            //parcours des taches
            for(Task t : HashTasksRealisable) {
                if(taskMax == null){
                    taskMax=t;
                }
                // Priorité LRPT (prendre la meilleur tâche)
                if (JobDuration[t.job]-JobDurationDone[t.job] > JobDuration[taskMax.job]-JobDurationDone[taskMax.job]) {
                    taskMax=t;
                }
            }
            //affectation de la tâche
            RorderSolution.addTask(instance.machine(taskMax),taskMax.job, taskMax.task);
            //On l'a supprimer de l'ensemble des tâche réalisable
            HashTasksRealisable.remove(taskMax);
            //mise à jour des tableau pour le calcul du temps est
            JobDurationDone[taskMax.job]+=instance.duration(taskMax);
            if(instance.numTasks-1>taskMax.task){
                //prochaine task réalisable est celle suivant la précédente
                HashTasksRealisable.add(new Task(taskMax.job,(taskMax.task +1)));
                // ajout de de la tâche dans l'ensemble des tâche déjà affecté
                HashTasksDone.add(new Task(taskMax.job,(taskMax.task)));
            }
        }


        return new Result(instance, RorderSolution.toSchedule(), Result.ExitCause.Blocked);
    }
}
