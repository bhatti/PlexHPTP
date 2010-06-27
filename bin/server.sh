if [ $# -gt 0 ];
then
  mvn 
fi;
rm -rf /tmp/plexhptp
rm cacerts
mvn exec:java -Dexec.mainClass="com.plexobject.hptp.service.FileServer"
