package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import java.util.*;
public class GloutonESTLRPT implements Solver {

    public Result solve(Instance instance, long deadline) {
        HashSet<Task> HashTasksRealisable = new HashSet<Task>();
        HashSet<Task> HashTasksMinimal = new HashSet<Task>();
        int JobDuration[] = new int [instance.numJobs];
        int JobDurationDone[] = new int [instance.numJobs];
        HashSet<Task> HashTasksDone = new HashSet<Task>();
        ResourceOrder RorderSolution = new ResourceOrder(instance);
        int [][] startTimes = new int [instance.numJobs][instance.numTasks];
        for(int i=0;i<instance.numJobs;i++){
            for(int j=0;j<instance.numTasks;j++){
                startTimes[i][j]=0;
            }
        }
        int[] releaseTimeOfMachine = new int[instance.numMachines];
        Arrays.fill(releaseTimeOfMachine,0);
        //init
        for(int job = 0 ; job<(instance.numJobs) ; job++) {
            Task t = new Task(job,0);
            HashTasksRealisable.add(t);
        }
        for(int j = 0 ; j < instance.numJobs ; j++) {
            for(int t = 0 ; t < instance.numTasks ; t++) {
                JobDuration[j] += instance.duration(j,t);
            }
        }

        while(!HashTasksRealisable.isEmpty()){
            HashTasksMinimal.clear();
            int minStartTime =-1;

            for(Task t : HashTasksRealisable) {
                if(minStartTime== -1){
                    minStartTime=t.task == 0 ? 0 : startTimes[t.job][t.task - 1] + instance.duration(t.job, t.task - 1);
                    minStartTime = Math.max(minStartTime, releaseTimeOfMachine[instance.machine(t)]);

                    HashTasksMinimal.add(t);
                }
                else {
                    //est1
                    int estT = t.task == 0 ? 0 : startTimes[t.job][t.task - 1] + instance.duration(t.job, t.task - 1);
                    estT = Math.max(estT, releaseTimeOfMachine[instance.machine(t)]);

                    if (estT < minStartTime) {
                        HashTasksMinimal.clear();
                        HashTasksMinimal.add(t);
                        minStartTime=estT;
                    }
                    else if (estT == minStartTime) {
                        HashTasksMinimal.add(t);
                    }

                }
            }
            Task taskMax = null;
            for(Task t : HashTasksDone) {
            }
            for(Task t : HashTasksMinimal) {
                if(taskMax == null){
                    taskMax=t;
                }
                if (JobDuration[t.job]-JobDurationDone[t.job] > JobDuration[taskMax.job]-JobDurationDone[taskMax.job]) {
                    taskMax=t;
                }
            }

            startTimes[taskMax.job][taskMax.task]=minStartTime;
            releaseTimeOfMachine[instance.machine(taskMax)]=minStartTime+instance.duration(taskMax);

            RorderSolution.addTask(instance.machine(taskMax),taskMax.job, taskMax.task);
            HashTasksRealisable.remove(taskMax);
            JobDurationDone[taskMax.job]+=instance.duration(taskMax);


            if(instance.numTasks-1>taskMax.task){
                HashTasksRealisable.add(new Task(taskMax.job,(taskMax.task +1)));
                HashTasksDone.add(new Task(taskMax.job,(taskMax.task)));
            }

        }


        return new Result(instance, RorderSolution.toSchedule(), Result.ExitCause.Blocked);
    }
}
