package com.filling.calculation.plugin.base.flink.transform;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.filling.calculation.common.CheckConfigUtil;
import com.filling.calculation.common.CheckResult;
import com.filling.calculation.flink.FlinkEnvironment;
import com.filling.calculation.flink.stream.FlinkStreamTransform;
import com.filling.calculation.flink.util.TableUtil;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;


public class FieldRemove implements FlinkStreamTransform<Row, Row> {


    private JSONObject config;

    private static String FIELD_NAME = "field";
    private static String FIELD = null;

    @Override
    public DataStream<Row> processStream(FlinkEnvironment env, DataStream<Row> dataStream) {

        StreamTableEnvironment tableEnvironment = env.getStreamTableEnvironment();

        return (DataStream<Row>) process(tableEnvironment, dataStream, "stream");
    }

    private Object process(TableEnvironment tableEnvironment, Object data, String type) {

        String sql = "select * from {table_name}"
            .replaceAll("\\{table_name}", config.getString(SOURCE_TABLE_NAME));

        Table table = tableEnvironment.sqlQuery(sql);
        // 判断参数是否为数组
        if(FIELD.startsWith("[")) {
            JSONArray _field = JSONArray.parseArray(FIELD);
            for (int i = 0; i < _field.size(); i++) {
                table = table.dropColumns( _field.getString(i) );
            }
        } else {
            table = table.dropColumns(FIELD);
        }

        return TableUtil.tableToDataStream((StreamTableEnvironment) tableEnvironment, table, false);
    }

    @Override
    public void setConfig(JSONObject config) {
        this.config = config;
    }

    @Override
    public JSONObject getConfig() {
        return config;
    }


    @Override
    public CheckResult checkConfig() {
       return CheckConfigUtil.check(config,FIELD_NAME );
    }

    @Override
    public void prepare(FlinkEnvironment env) {
        FIELD = config.getString(FIELD_NAME);
    }
}
