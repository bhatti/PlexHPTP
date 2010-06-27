rm -rf tmp
mkdir tmp
cd tmp
jar -xf ../target/hptp-0.1-SNAPSHOT-jar-with-dependencies.jar 
#rm -rf javax
jar -cf ../target/hptp-0.1-SNAPSHOT-jar-with-dependencies.jar *
cd ..
rm -rf tmp
/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/bin/jarsigner -keystore plexhptp.jks -storepass plexhptp -keypass plexhptp target/hptp-0.1-SNAPSHOT-jar-with-dependencies.jar plexhptp
#/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/bin/jarsigner -verify target/hptp-0.1-SNAPSHOT-jar-with-dependencies.jar


cd target
ln -s ../src/main/html/index.html
open index.html 
