# Phang Teng Fone 1003296
### ReadMe for TODO#4 main_loop()

The initial codes were given to read the file line by line in a while loop.

Allowed condition: Busy Waiting

The shared memory job consist of a number of space N. Where each child process will enter into each slot of the empty space N. The main_loop() will be the one that will check the job buffer, i.e if there is a new input, the main process will check if the task status of the particular child is 0 [job done]. And base on the condition [job_dispatch()] it will assign the input into the child, by executing a semphamore post on that buffer slot. If another input is called, it will busy wait and check each status of the buffer as long as there are still inputs from the file. 

The while found_slot is true loops examine each slot of the buffer with accordance to the number of processes. It locks down the status of each child processes by using the function ```waitpid```. If the job has done running and the child is alive, it will post the input to the semphamore whilst updating the task content then break out of the loop and continue onto the next line (input). 

However if the child is terminated due to an illegal task, we revive them by forking and creating a new worker and dispatch a job using ```job_dispatch()``` to the worker shown: 
```c                     
                    pid_t pid;
                    pid = fork(); // both parent & child return from fork()
                    children_processes[i] = pid;
                    if (pid < 0)
                    { // error
                        // fprintf(stderr, "Fork Failed");
                        exit(-1);
                    }
                    else if (pid == 0)
                    { // child
                        job_dispatch(i);
                        break;
                    }
                    else
                    { // parent
                        shmPTR_jobs_buffer[i].task_duration = num;
                        shmPTR_jobs_buffer[i].task_type = action;
                        shmPTR_jobs_buffer[i].task_status = 1; // set to 1
                        found_slot = 1;
                        sem_post(sem_jobs_buffer[i]);
                        break;
                    }
```
Once all the input have been taken in, there is a need to terminate all the worker that are currently alive.

This code uses the total number of terminated process to check if it is the same as the total number of initial process to ensure that all processes are accounted for before the termination of the program. We constantly check for the number of dead process to be equals to the number of process created. When the child is detected as alive and task complete, we set the content of the task to ```'z'``` which will send the child for a proper legal exit (terminate process). Not all alive process have no jobs or cleared their job, thus we are require to busy wait. If the child is not alive, it will increase the number of terminating process remainder. Once the number of terminating process remainder is equivalent to the number of process, it will exit the loop. 

```c
    int remainder = 0;
    while (remainder < number_of_processes)
    {
        remainder = 0; // reset each time to ensure all processes are marked for 'z' or has already been terminated
        for (int i = 0; i < number_of_processes; i++)
        {
            int alive = waitpid(children_processes[i], NULL, WNOHANG);
            if (alive == 0) // check if process is alive
            {
                if (shmPTR_jobs_buffer[i].task_status == 0)  // Check if child has completed job
                {
                    shmPTR_jobs_buffer[i].task_type = 'z'; // ensure task type for legal termination
                    shmPTR_jobs_buffer[i].task_duration = 0;
                    shmPTR_jobs_buffer[i].task_status = 1;
                    sem_post(sem_jobs_buffer[i]); // Semaphore release for job_dispatch() job
                }
            }
            else
                remainder++; // accounts for terminated process
        }
    }
```