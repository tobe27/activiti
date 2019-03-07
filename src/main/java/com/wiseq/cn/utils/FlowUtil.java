package com.wiseq.cn.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: FlowUtil
 **/
public class FlowUtil {

    /**
     *  解析Request中的所有参数
     * @param request
     * @return
     */
    public static Map<String, Object> parseRequestParams(HttpServletRequest request){
        Map<String, Object> newMap = new HashMap<>();
        Map<String, String[]> map = request.getParameterMap();
        Set keSet = map.entrySet();
        for(Iterator itr = keSet.iterator(); itr.hasNext();){
            Map.Entry me = (Map.Entry)itr.next();
            Object key = me.getKey();
            Object ov = me.getValue();
            String[] value;
            if(ov instanceof String[]){
                value = (String[])ov;
                newMap.put(key.toString(), value[0]);
            }else{
                newMap.put(key.toString(), ov.toString());
            }
        }
        // 移除流程KEY
        newMap.remove("processKey");
        return newMap;
    }

    /**
     * 获取生成的ID
     * @return
     */
    public static String getId(){
        String id = UUID.randomUUID().toString();
        return id.replaceAll("-", "");
    }
}
