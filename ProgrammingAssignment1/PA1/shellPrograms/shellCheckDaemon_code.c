#include "shellPrograms.h"
#define FILENAME "output.txt"
/*  A program that prints how many summoned daemons are currently alive */
int shellCheckDaemon_code()
{
   /* TASK 8 */
   //Create a command that trawl through output of ps -efj and contains "summond"
   char *command = malloc(sizeof(char) * 256);
   sprintf(command, "ps -efj | grep summond  | grep -v tty > output.txt");

   // TODO: Execute the command using system(command) and check its return value
   if (system(command) == -1) {
      printf("return -1\n");
      exit(1);
   }

   free(command);

   // TODO: Analyse the file output.txt, wherever you set it to be. You can reuse your code for countline program
   // 1. Open the file
   // 2. Fetch line by line using getline()
   FILE *fp; 
   ssize_t line_size;
   size_t size = SHELL_BUFFERSIZE;
   char *buffer = malloc(sizeof(char) * size);
   int line_count = 0;
   fp = fopen(FILENAME, "r");
   if (fp == NULL)
   {
      printf("File does not exist\n");
      exit(1);
   }
   line_size = getline(&buffer, &size, fp);
   while (line_size >= 0)
   {
      line_count++;
      line_size = getline(&buffer, &size, fp);
   }
   free(buffer);
   buffer = NULL;
   fclose(fp);
   
   if (line_count == 0)
      printf("No daemon is alive right now\n");
   else
   {
      printf("There are in total of %d live daemons \n", line_count);
   }
   // TODO: close any file pointers and free any statically allocated memory
   /* Free the allocated line buffer */
   return 1;
}

int main(int argc, char **args)
{
   return shellCheckDaemon_code();
}