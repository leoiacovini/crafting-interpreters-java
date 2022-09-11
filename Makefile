#all: compile

gen:
	java -classpath ./target/classes com.leoiacovini.tool.ExprGenerator ./src/main/java/com/leoiacovini/lox

repl:
	java -classpath ./target/classes com.leoiacovini.lox.Main

run:
	java -classpath ./target/classes com.leoiacovini.lox.Main $(ARGS)

compile:
	mvn compile

clean:
	mvn clean