@echo off
echo Compilando o sistema mais recente na versao atualizada..
javac -d bin -cp "lib/*" -sourcepath src src/br/com/criptovision/main/Main.java

echo Abrindo Menu Principal...
java -cp "bin;lib/*" br.com.criptovision.main.Main
pause