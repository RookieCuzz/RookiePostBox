package com.cuzz.rookiepostbox.database;

import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.PostBox;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

    public static ConcurrentHashMap<String,PostBox> postBoxes =new ConcurrentHashMap<>();
}
