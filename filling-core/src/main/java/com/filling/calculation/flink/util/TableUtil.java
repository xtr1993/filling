package com.filling.calculation.flink.util;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.types.Row;

public class TableUtil {

    public static DataStream<Row> tableToDataStream(StreamTableEnvironment tableEnvironment, Table table, boolean isAppend) {

        TypeInformation<Row> typeInfo = table.getSchema().toRowType();
        if (isAppend) {
            return tableEnvironment.toAppendStream(table, typeInfo);
        } else {
            return tableEnvironment
                    .toRetractStream(table, typeInfo)
                    .filter(row -> row.f0)
                    .map(row -> row.f1)
                    .returns(typeInfo);
        }
    }

    public static boolean tableExists(TableEnvironment tableEnvironment, String name){
        String currentCatalog = tableEnvironment.getCurrentCatalog();
        Catalog catalog = tableEnvironment.getCatalog(currentCatalog).get();
        ObjectPath objectPath = new ObjectPath(tableEnvironment.getCurrentDatabase(), name);
        return catalog.tableExists(objectPath);
    }
}
