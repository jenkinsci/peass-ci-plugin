timestamp=$(date +%s)
mkdir -p /nfs/user/do820mize/rcalogs/demo-project-gradle_7_3_3-java17
sbatch --partition=galaxy-low-prio --nice=1000000 --time=10-0 --output=/nfs/user/do820mize/rcalogs/demo-project-gradle_7_3_3-java17/-1_ExampleTest#test_$timestamp.out --export=PROJECT=https://github.com/mawHBT/demo-project-gradle_7_3_3-java17.git,HOME=/nfs/user/do820mize,START=65aba5fbd67a97362ff922c3660445337ba2c1d0,END=65aba5fbd67a97362ff922c3660445337ba2c1d0,INDEX=-1,ITERATIONS=4,REPETITIONS=2,VMS=30,EXPERIMENT_ID=default,TEST=demo.project.gradle.ExampleTest#test executeRCA.sh
