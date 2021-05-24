package com.github.rerorero.kafka.jsonpath;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.AbstractJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import javafx.util.Pair;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Test {
    static class TaskState {
        List<Pair<String, Object>> current;
    }

    interface Task {
        void apply(TaskState state);
    }

    static class GetFromStructTask implements Task {
        final String fieldName;

        GetFromStructTask(String fieldName) {
            this.fieldName = fieldName;
        }

        public void apply(TaskState state) {
            state.current = state.current.stream()
                    .map(cur -> new Pair<String, Object>(cur.getKey() + "." + fieldName, ((Struct) cur.getValue()).get(fieldName)))
                    .collect(Collectors.toList());
        }
    }

    static class GetFromArrayTask implements Task {
        final Integer index;

        GetFromArrayTask(Integer index) {
            this.index = index;
        }

        public void apply(TaskState state) {
            List<Pair<String, Object>> updated = new ArrayList<>();
            for (Pair<String, Object> current : state.current) {
                List<Object> curList = (List<Object>) current.getValue();
                for (int i = 0; i < curList.size(); i++) {
                    if (index != null && index.intValue() != i) {
                        continue;
                    }
                    updated.add(new Pair(current.getKey() + "[" + i + "]", curList.get(i)));
                }
            }
            state.current = updated;
        }
    }

    // update

    static class UpdateTaskState {
        Map<String, Object> newValue;
        List<Pair<String, Object>> current;
    }

    interface UpdateTask {
        void apply(UpdateTaskState state);
    }

    private static Struct copyStruct(Struct org) {
        Struct newStruct = new Struct(org.schema());
        for (Field field: org.schema().fields()) {
            Object obj = org.get(field);
            Schema fieldSchema = field.schema();
            if (obj == null) {
                continue;
            } else if (fieldSchema.type() == Schema.Type.STRUCT) {
                newStruct.put(field, copyStruct((Struct) obj));
            } else if (fieldSchema.type() == Schema.Type.ARRAY) {
                newStruct.put(field, copyList((List<Object>)obj, fieldSchema));
            } else if (fieldSchema.type() == Schema.Type.MAP) {
                throw new UnsupportedOperationException("MAP is not supported yet");
            } else {
                newStruct.put(field, obj);
            }
        }
        return newStruct;
    }

    private static List<Object> copyList(List<Object> org, Schema schema) {
        Schema valueSchema = schema.valueSchema();
        return org.stream().map(o -> {
            if (valueSchema.type() == Schema.Type.STRUCT) {
                return copyStruct((Struct) o);
            } else if (valueSchema.type() == Schema.Type.MAP) {
                throw new UnsupportedOperationException("MAP is not supported yet");
            } else if (valueSchema.type() == Schema.Type.ARRAY) {
                throw new UnsupportedOperationException("ARRAY in ARRAY is not supported yet");
            } else {
                return o;
            }
        }).collect(Collectors.toList());
    }

    static class UpdateStructTask implements UpdateTask {
        final String fieldName;

        UpdateStructTask(String fieldName) {
            this.fieldName = fieldName;
        }

        public void apply(UpdateTaskState state) {
            List<Pair<String, Object>> updatedState = new ArrayList<>();

            for (Pair<String, Object> cur : state.current) {
                Struct parent = (Struct) cur.getValue();

                String field = cur.getKey() + "." + fieldName;
                Object newVal = state.newValue.get(field);
                if (newVal != null) {
                    // update directly if field matches the newValue path
                    parent.put(fieldName, newVal);
                    updatedState.add(new Pair(field, newVal));
                } else {
                    // otherwise copy and return subscript
                    updatedState.add(new Pair(field, parent.get(fieldName)));
                }
            }

            state.current = updatedState;
        }
    }

    static class UpdateArrayTask implements UpdateTask {
        final Integer index;

        UpdateArrayTask(Integer index) {
            this.index = index;
        }

        public void apply(UpdateTaskState state) {
            List<Pair<String, Object>> updated = new ArrayList<>();
            for (Pair<String, Object> current : state.current) {
                List<Object> curList = (List<Object>) current.getValue();
                for (int i = 0; i < curList.size(); i++) {
                    if (index != null && index.intValue() != i) {
                        continue;
                    }
                    String path = current.getKey() + "[" + i + "]";
                    Object newVal = state.newValue.get(path);
                    if (newVal != null) {
                        curList.set(i, newVal);
                        updated.add(new Pair(path, newVal));
                    } else {
                        updated.add(new Pair(path, curList.get(i)));
                    }
                }
            }
            state.current = updated;
        }
    }


    static class TestListener extends JsonPathBaseListener {
        String paths = "$";
        LinkedList<Task> tasks = new LinkedList<>();

        void appendPath(String s) {
            paths += s;
        }

        private String unquoteSTRING(TerminalNode node) {
            String s = node.getText();
            return s.substring(1, s.length() - 1);
        }

        private void parseArraySubscript(JsonPathParser.ArraySubContext ctx) {
            if (ctx == null) {
                return;
            }
            if (ctx.NUMBER() != null) {
                appendPath("[" + ctx.NUMBER().getText() + "]");
                tasks.add(new GetFromArrayTask(Integer.parseInt(ctx.NUMBER().getText())));
            } else if (ctx.WILDCARD() != null) {
                appendPath("[" + ctx.WILDCARD().getText() + "]");
                tasks.add(new GetFromArrayTask(null));
            }
        }

        @Override
        public void exitSubscriptBracket(JsonPathParser.SubscriptBracketContext ctx) {
            String field = unquoteSTRING(ctx.STRING());
            appendPath(ctx.BRACKET_LEFT().getText() + field + ctx.BRACKET_RIGHT().getText());
            tasks.add(new GetFromStructTask(field));
            parseArraySubscript(ctx.arraySub());
        }

        @Override
        public void exitSubscriptDot(JsonPathParser.SubscriptDotContext ctx) {
            appendPath(ctx.SUBSCRIPT_DOT().getText() + ctx.ID().getText());
            tasks.add(new GetFromStructTask(ctx.ID().toString()));
            parseArraySubscript(ctx.arraySub());
        }
    }

    static class UpdateTestListener extends JsonPathBaseListener {
        String paths = "$";
        LinkedList<UpdateTask> tasks = new LinkedList<>();

        void appendPath(String s) {
            paths += s;
        }

        private String unquoteSTRING(TerminalNode node) {
            String s = node.getText();
            return s.substring(1, s.length() - 1);
        }

        private void parseArraySubscript(JsonPathParser.ArraySubContext ctx) {
            if (ctx == null) {
                return;
            }
            if (ctx.NUMBER() != null) {
                appendPath("[" + ctx.NUMBER().getText() + "]");
                tasks.add(new UpdateArrayTask(Integer.parseInt(ctx.NUMBER().getText())));
            } else if (ctx.WILDCARD() != null) {
                appendPath("[" + ctx.WILDCARD().getText() + "]");
                tasks.add(new UpdateArrayTask(null));
            }
        }

        @Override
        public void exitSubscriptBracket(JsonPathParser.SubscriptBracketContext ctx) {
            String field = unquoteSTRING(ctx.STRING());
            appendPath(ctx.BRACKET_LEFT().getText() + field + ctx.BRACKET_RIGHT().getText());
            tasks.add(new UpdateStructTask(field));
            parseArraySubscript(ctx.arraySub());
        }

        @Override
        public void exitSubscriptDot(JsonPathParser.SubscriptDotContext ctx) {
            appendPath(ctx.SUBSCRIPT_DOT().getText() + ctx.ID().getText());
            tasks.add(new UpdateStructTask(ctx.ID().toString()));
            parseArraySubscript(ctx.arraySub());
        }
    }

    private static JsonPathParser newParser(String text) {
        CharStream cs = CharStreams.fromString(text);
        // CharStreamをlexerに渡す
        JsonPathLexer lexer = new JsonPathLexer(cs);
        // lexerでトークン列に分解
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        // トークン列をparserに渡し、ASTを作る
        return new JsonPathParser(tokens);
    }

    public static void go() {
        // 文字列からCharStreamを生成
//        CharStream cs = CharStreams.fromString("$.buz.arr[*].haha");
//        String text = "$.buz.foo";
        String text = "$.buz.arr[*].haha";
        JsonPathParser parser = newParser(text);
        // listenerでASTをトラバース
        ParseTreeWalker walker = ParseTreeWalker.DEFAULT;
        TestListener listener = new TestListener();
        walker.walk(listener, parser.jsonpath());


        final Schema scAryElem = SchemaBuilder.struct()
                .field("haha", Schema.STRING_SCHEMA)
                .build();
        final Schema scBuz = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("arr", SchemaBuilder.array(scAryElem))
                .build();
        final Schema scRoot = SchemaBuilder.struct()
                .field("buz", scBuz)
                .field("int", SchemaBuilder.INT32_SCHEMA)
                .build();

        // new data
        Struct arr1 = new Struct(scAryElem);
        arr1.put("haha", "haha1 bingo");
        Struct arr2 = new Struct(scAryElem);
        arr2.put("haha", "haha2 bingo");

        Struct orgBuz = new Struct(scBuz);
        orgBuz.put("foo", "bingo");
        orgBuz.put("arr", Arrays.asList(arr1, arr2));
        Struct org = new Struct(scRoot);
        org.put("buz", orgBuz);
        org.put("int", 100);

        TaskState ts = new TaskState();
        ts.current = Arrays.asList(new Pair<String, Object>("$", org));
        for (Task task : listener.tasks) {
            task.apply(ts);
        }
        System.out.println("natoring task state=" + ts.current.toString());
        System.out.println("natoring path=" + listener.paths);

        // UPDATE
        Map<String, Object> newValue = new HashMap<>();
        for (Pair<String, Object> p : ts.current) {
            newValue.put(p.getKey(), "-updated!!=" +p.getValue()+ "=");
        }
        UpdateTaskState uts = new UpdateTaskState();
        Struct updated = copyStruct(org);
        uts.current = Arrays.asList(new Pair<String, Object>("$", updated));
        uts.newValue = newValue;
        UpdateTestListener updateListener = new UpdateTestListener();
        JsonPathParser parser2 = newParser(text);
        walker.walk(updateListener, parser2.jsonpath());
        for (UpdateTask task : updateListener.tasks) {
            task.apply(uts);
        }

        System.out.println("natoring updatetask state=" + uts.current.toString());
        System.out.println("natoring update path=" + updateListener.paths);
        System.out.println("natoring current" + uts.current);
        System.out.println("natoring updated" + updated);
        System.out.println("natoring org" + org);
    }
}
