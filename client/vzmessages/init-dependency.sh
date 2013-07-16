echo "Maven Deployer";

confirm()
{
echo "Is deployed in maven repository (yes/no) ?"
read  ans
        if [ "$ans" == "n" ] || [ "$ans" == 'N' ] || [ "$ans" == "No" ] || [ "$ans" == "no" ];
        then
                exit 1;
        else
                if [ "$ans" == "y" ] || [ "$ans" == "Yes" ]  || [ "$ans" == "Y" ] ;
                then
                        echo " done ";
                else
                        echo " enter yes/no ?" ;
                        confirm;
                fi
        fi
}

confirm
# Android .............................

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=2.1   -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-7\android.jar"
confirm 
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=2.2   -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-8\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=2.3.3 -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-10\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=3.0   -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-11\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=3.1   -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-12\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=3.2   -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-13\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=4.0   -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-14\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=4.0.3 -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-15\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=4.1   -Dpackaging=jar -Dfile="$ANDROID_HOME\platforms\android-16\android.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android-support-v4 -Dversion=v4   -Dpackaging=jar -Dfile="$ANDROID_HOME\extras\android\support\v4\android-support-v4.jar"
confirm
mvn install:install-file -DgroupId=android -DartifactId=android-support-v13 -Dversion=v13   -Dpackaging=jar -Dfile="$ANDROID_HOME\extras\android\support\v13\android-support-v13.jar"
confirm

# ThridParty jar file 

mvn install:install-file -DgroupId=acra -DartifactId=acra-custom -Dversion=4.3.0           -Dpackaging=jar -Dfile="libs\acra-4.3.0-custom.jar"
confirm
mvn install:install-file -DgroupId=anm-library -DartifactId=anm-library -Dversion=1.0        -Dpackaging=jar -Dfile="libs\ANMLibrary.jar"
confirm
mvn install:install-file -DgroupId=cmlibrary -DartifactId=jmmslib -Dversion=1.0.0.3        -Dpackaging=jar -Dfile="libs\jmmslib.jar"
confirm
mvn install:install-file -DgroupId=cmlibrary -DartifactId=spamrep -Dversion=1.0.0.3        -Dpackaging=jar -Dfile="libs\spamrep.jar"
confirm
mvn install:install-file -DgroupId=com.vzw.navigation -DartifactId=nbi -Dversion=3.2.2.29   -Dpackaging=jar -Dfile="/var/lib/jenkins/jobs/ANDROID_TRUNK_VZM_4.0/workspace/VZMessages-New/libs/libs/nbi.jar"
confirm
mvn install:install-file -DgroupId=log4j -DartifactId=log4j-android -Dversion=1.2.16   -Dpackaging=jar -Dfile="libs\log4j-1.2.16.jar"
confirm
mvn install:install-file -DgroupId=libns -DartifactId=libns -Dversion=1.0.0.3   -Dpackaging=so -Dclassifier=armeabi  -Dfile="libs\armeabi\libNS.so"
confirm

#========================= VMA Dependency ================================
confirm
mvn install:install-file -DgroupId=activation -DartifactId=activation -Dversion=1.0   -Dpackaging=jar -Dfile="..\vzm\trunk\vzm\vma-sync-lib\libs\activation.jar"
confirm
mvn install:install-file -DgroupId=additionnal -DartifactId=additionnal -Dversion=1.0   -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\additionnal.jar"
confirm
mvn install:install-file -DgroupId=apache-mime4j-core -DartifactId=apache-mime4j-core -Dversion=0.7   -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\apache-mime4j-core-0.7-SNAPSHOT.jar"
confirm
mvn install:install-file -DgroupId=apache-mime4j-dom -DartifactId=apache-mime4j-dom -Dversion=0.7   -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\apache-mime4j-dom-0.7-SNAPSHOT.jar"
confirm
mvn install:install-file -DgroupId=commons-io -DartifactId=commons-io -Dversion=2.0.1   -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\commons-io-2.0.1.jar"
confirm
mvn install:install-file -DgroupId=htmlcleaner -DartifactId=htmlcleaner -Dversion=2.2   -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\htmlcleaner-2.2.jar"
confirm
mvn install:install-file -DgroupId=imap-msa -DartifactId=imap-msa -Dversion=4.0  -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\imap-msa.jar"
confirm
mvn install:install-file -DgroupId=jzlib -DartifactId=jzlib -Dversion=1.0.7  -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\jzlib-1.0.7.jar"
confirm
mvn install:install-file -DgroupId=mail -DartifactId=mail -Dversion=4.0  -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\mail.jar"
confirm
mvn install:install-file -DgroupId=mailapi -DartifactId=mailapi -Dversion=4.0  -Dpackaging=jar -Dfile="..\vzm\vma-sync-lib\libs\mailapi.jar"

chown -Rv jenkins  /strumsoft/maven/local/repo/

echo "Deployed"


