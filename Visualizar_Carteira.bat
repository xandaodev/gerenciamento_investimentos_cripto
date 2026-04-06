@echo off
echo Compilando o Ticker View...
javac -d bin -cp "lib/*" -sourcepath src src/br/com/criptovision/main/TickerView.java

echo Abrindo Resumo da Carteira...
java -cp "bin;lib/*" br.com.criptovision.main.TickerView
pause