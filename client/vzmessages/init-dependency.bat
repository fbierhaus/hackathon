

# Android .............................

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=2.1   -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-7\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=2.2   -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-8\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=2.3.3 -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-10\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=3.0   -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-11\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=3.1   -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-12\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=3.2   -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-13\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=4.0   -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-14\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=4.0.3 -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-15\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=4.1   -Dpackaging=jar -Dfile="%ANDROID_HOME%\platforms\android-16\android.jar"

mvn install:install-file -DgroupId=android -DartifactId=android-support-v4 -Dversion=v4   -Dpackaging=jar -Dfile="%ANDROID_HOME%\extras\android\support\v4\android-support-v4.jar"

mvn install:install-file -DgroupId=android -DartifactId=android-support-v13 -Dversion=v13   -Dpackaging=jar -Dfile="%ANDROID_HOME%\extras\android\support\v13\android-support-v13.jar"


# ThridParty jar file 

mvn install:install-file -DgroupId=acra -DartifactId=acra-custom -Dversion=4.3.0           -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\branch\VZMessages_3.1_merge_27139\VZMessages-New\libs\acra-4.3.0-custom.jar"

mvn install:install-file -DgroupId=anm-library -DartifactId=anm-library -Dversion=1.0        -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\branch\VZMessages_3.1_merge_27139\VZMessages-New\libs\ANMLibrary.jar"

mvn install:install-file -DgroupId=cmlibrary -DartifactId=jmmslib -Dversion=1.0.0.3        -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\branch\VZMessages_3.1_merge_27139\VZMessages-New\libs\jmmslib.jar"

mvn install:install-file -DgroupId=cmlibrary -DartifactId=spamrep -Dversion=1.0.0.3        -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\branch\VZMessages_3.1_merge_27139\VZMessages-New\libs\spamrep.jar"

mvn install:install-file -DgroupId=com.vzw.navigation -DartifactId=nbi -Dversion=1.2.16   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\branch\VZMessages_3.1_merge_27139\VZMessages-New\libs\nbi.jar"

mvn install:install-file -DgroupId=log4j -DartifactId=log4j-android -Dversion=1.2.16   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\branch\VZMessages_3.1_merge_27139\VZMessages-New\libs\log4j-1.2.16.jar"

mvn install:install-file -DgroupId=libns -DartifactId=libns -Dversion=1.0.0.3   -Dpackaging=so -Dclassifier=armeabi  -Dfile="D:\jega\sourcecode\vzm\branch\VZMessages_3.1_merge_27139\VZMessages-New\libs\armeabi\libNS.so"
confirm

========================= VMA Dependency ================================

mvn install:install-file -DgroupId=activation -DartifactId=activation -Dversion=1.0   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\activation.jar"

mvn install:install-file -DgroupId=additionnal -DartifactId=additionnal -Dversion=1.0   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\additionnal.jar"

mvn install:install-file -DgroupId=apache-mime4j-core -DartifactId=apache-mime4j-core -Dversion=0.7   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\apache-mime4j-core-0.7-SNAPSHOT.jar"

mvn install:install-file -DgroupId=apache-mime4j-dom -DartifactId=apache-mime4j-dom -Dversion=0.7   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\apache-mime4j-dom-0.7-SNAPSHOT.jar"

mvn install:install-file -DgroupId=commons-io -DartifactId=commons-io -Dversion=2.0.1   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\commons-io-2.0.1.jar"

mvn install:install-file -DgroupId=htmlcleaner -DartifactId=htmlcleaner -Dversion=2.2   -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\htmlcleaner-2.2.jar"

mvn install:install-file -DgroupId=imap-msa -DartifactId=imap-msa -Dversion=4.0  -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\imap-msa.jar"

mvn install:install-file -DgroupId=jzlib -DartifactId=jzlib -Dversion=1.0.7  -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\jzlib-1.0.7.jar"

mvn install:install-file -DgroupId=mail -DartifactId=mail -Dversion=4.0  -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\mail.jar"

mvn install:install-file -DgroupId=mailapi -DartifactId=mailapi -Dversion=4.0  -Dpackaging=jar -Dfile="D:\jega\sourcecode\vzm\trunk\vzm\vma-sync-lib\libs\mailapi.jar"
