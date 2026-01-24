package com.sda.books.reader.db


//import android.database.sqlite.SQLiteDatabase
//import android.database.sqlite.SQLiteDatabase
import android.content.Context
import android.content.res.AssetManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils
import com.sda.books.reader.App
import com.sda.books.reader.entity.Category
import com.sda.books.reader.entity.Chapter
import com.sda.books.reader.entity.Volume
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.GsonUtils
import com.sda.books.reader.util.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class DatabaseHelper private constructor(private val context: Context) {
    companion object {
        private const val TAG = "DatabaseHelper"
        const val DB_NAME = "xshj.db" // 数据库文件名
        private var instance: DatabaseHelper? = null
        private var customDbPath: String? = null
        public var isFinish = false

        fun getInstance(): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(App.context)
            }
            return instance!!
        }


        fun setDatabasePath(path: String) {
            customDbPath = path
            instance = null // 强制下次获取实例时重新初始化
        }
    }

    private var database: SQLiteDatabase? = null

    //    private val dbPath: String = context.getDatabasePath(DB_NAME).path

    private var dbPath: String = customDbPath ?: context.getDatabasePath(DB_NAME).path
    init {
        dbPath =
            PreferenceHelper(App.context).currentDbPath ?: context.getDatabasePath(DB_NAME).path

        // 验证数据库路径
        val dbFile = File(dbPath)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            Log.e(TAG, "数据库文件不存在或为空: $dbPath")
            // 回退到默认数据库
            if (customDbPath != null) {
                Log.w(TAG, "回退到默认数据库")
                customDbPath = null
                // 更新 SharedPreferences 中的路径
                PreferenceHelper(App.context).currentDbPath = context.getDatabasePath(DB_NAME).path
            }
        }
    }

    /**
     * 从 assets 复制数据库到内部存储
     */
    private suspend fun copyDatabaseFromAssets() = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        //LogUtils.e("复制数据库到手机文件夹01"+dbFile.absolutePath)
        // 如果数据库文件已存在且完整，不需要复制
        if (dbFile.exists() && dbFile.length() > 0) {
            //Log.d(TAG, "数据库已存在，跳过复制")
            return@withContext
        }
        //LogUtils.e("复制数据库到手机文件夹02")
        // 确保数据库目录存在
        val dbDir = dbFile.parentFile
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        //LogUtils.e("复制数据库到手机文件夹03")

        try {

            context.assets.open(DB_NAME).use { inputStream ->

                FileOutputStream(dbFile).use { outputStream ->
                    //LogUtils.e("复制数据库到手机文件夹08")
                    val buffer = ByteArray(20480)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    //isFinish = true
                }
            }
        } catch (e: IOException) {
            //LogUtils.e("复制数据库到手机文件夹06"+e.message)
            //Log.e(TAG, "复制数据库失败: ${e.message}", e)
            throw e
        }
    }


    /**
     * 异步复制数据库（在后台线程执行）
     */
    suspend fun copyDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        if (!checkDatabaseExists()) {
            try {
                copyDatabase()
                //Log.d(TAG, "数据库复制成功000009")
            } catch (e: Exception) {
                //Log.e(TAG, "复制数据库失败: ${e.message}", e)
                throw e
            }
        }
    }

    public fun checkDatabaseExists(): Boolean {
        return File(dbPath).exists()
    }

    @Throws(IOException::class)
    private fun copyDatabase() {
        context.assets.open(DB_NAME).use { inputStream ->
            FileOutputStream(dbPath).use { outputStream ->
                val buffer = ByteArray(10240) // 增大缓冲区提高复制速度
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
        }
    }

    /**
     * 打开数据库（自动处理异步复制）
     */
    suspend fun openDatabase(): SQLiteDatabase {
        return withContext(Dispatchers.IO) {
            try {

                // 确保数据库已复制
                copyDatabaseFromAssets()
                LogUtils.e("数据库路劲不001===========${dbPath}")
                LogUtils.e("数据库路劲不001===========${checkDatabase()}")

                // 打开数据库
                database = SQLiteDatabase.openDatabase(
                    dbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                LogUtils.e("数据库路劲不002===========${dbPath}")
                database!!.apply {
                    Log.d(TAG, "数据库已打开")
                }
            } catch (e: Exception) {
                Log.e(TAG, "打开数据库失败: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * 检查数据库是否存在
     */
    private fun checkDatabase(): Boolean {
        return File(dbPath).exists()
    }


    /**
     * 关闭数据库连接
     */
    fun closeDatabase() {
        database?.apply {
            if (isOpen) {
                close()
                Log.d(TAG, "数据库已关闭")
            }
        }
        database = null
    }

    suspend fun queryCategoryList(): List<Category> {
        var nResult = listOf<Category>()
        withContext(Dispatchers.IO) {
            val cursor = query(
                "category",
                columns = arrayOf("id", "cateName"),
                //selection = "parentId = ?",
                //selectionArgs = arrayOf("0")
            )
            cursor?.use {
                val result = mutableListOf<Category>()
                while (it.moveToNext()) {
                    val id = it.getInt(it.getColumnIndexOrThrow("id"))
                    val name = it.getString(it.getColumnIndexOrThrow("cateName"))
                    LogUtils.e("09============>>>${name}")


                    if(!name.contains("/")){
                        val category = Category().apply {
                            this.id = id
                            this.cateName = name
                        }
                        result.add(category)
                    }
                    LogUtils.e("09============>>>${result}")

                }
                nResult = result.sortedWith(Comparator { o1, o2 ->
                    try {

                        val regex = Regex("^\\d{1,3}-")  // 注意：Kotlin 字符串中反斜杠需要转义
                        val matchResult = regex.find(o1.cateName)
                        val index1Match = (matchResult?.value) ?: ""
                        val match2Result = regex.find(o2.cateName)
                        val index2Match = (match2Result?.value) ?: ""

                        val index1 = index1Match.toInt()
                        val index2 = index2Match.toInt()

                        // 正确的比较方式
                        return@Comparator index1.compareTo(index2)
                    } catch (e: Exception) {
                        // 更健壮的异常处理
                        val o1Valid = isValidVolName(o1.cateName)
                        val o2Valid = isValidVolName(o2.cateName)

                        return@Comparator when {
                            o1Valid && !o2Valid -> 1           // 有效名称排在无效名称之后
                            !o1Valid && o2Valid -> -1          // 无效名称排在有效名称之前
                            else -> o1.cateName.compareTo(o2.cateName)  // 都无效时按名称字符串比较
                        }
                    }
                })
            }
        }
        return nResult.sortedBy { it.id }
    }

    fun querySubCategory1(parentName: String): List<Category> {
        var nResult = listOf<Category>()
        val cursor = query(
            "category",
            columns = arrayOf("id", "cateName"),
            //selection = "parentId = ?",
            //selectionArgs = arrayOf("$parentId")
        )
        //LogUtils.e("=========>>>>>>>>>${parentName}")
        cursor?.use {
            val result = mutableListOf<Category>()
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val name = it.getString(it.getColumnIndexOrThrow("cateName"))

                if(name.contains("${parentName}/")){
                    val category = Category().apply {
                        this.id = id
                        this.cateName = name.replace("${parentName}/","")
                    }
                    result.add(category)
                }

            }

            nResult = result.sortedWith(Comparator { o1, o2 ->
                try {
                    val regex = Regex("^\\d{1,3}-")  // 注意：Kotlin 字符串中反斜杠需要转义
                    val matchResult = regex.find(o1.cateName)
                    val index1Match = (matchResult?.value) ?: ""
                    val match2Result = regex.find(o2.cateName)
                    val index2Match = (match2Result?.value) ?: ""

                    val index1 = index1Match.toInt()
                    val index2 = index2Match.toInt()

                    // 正确的比较方式
                    return@Comparator index1.compareTo(index2)
                } catch (e: Exception) {
                    // 更健壮的异常处理
                    val o1Valid = isValidVolName(o1.cateName)
                    val o2Valid = isValidVolName(o2.cateName)

                    return@Comparator when {
                        o1Valid && !o2Valid -> 1           // 有效名称排在无效名称之后
                        !o1Valid && o2Valid -> -1          // 无效名称排在有效名称之前
                        else -> o1.cateName.compareTo(o2.cateName)  // 都无效时按名称字符串比较
                    }
                }
                return@Comparator 0
            })
        }
        return nResult.sortedBy { it.id }
    }

    fun querySubCategoryName(parentId: String): List<Category> {
        var nResult = listOf<Category>()
        val cursor = query(
            "category",
            columns = arrayOf("id", "cateName"),
            selection = "cateName like ?",
            selectionArgs = arrayOf("$parentId")
        )
        cursor?.use {
            val result = mutableListOf<Category>()
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                var name = it.getString(it.getColumnIndexOrThrow("cateName"))
                if(name.contains("/")){
                    name = name.split("/")[1]
                }
                val category = Category().apply {
                    this.id = id
                    this.cateName = name
                }
                result.add(category)
            }

            nResult = result.sortedWith(Comparator { o1, o2 ->
                try {
                    val regex = Regex("^\\d{1,3}-")  // 注意：Kotlin 字符串中反斜杠需要转义
                    val matchResult = regex.find(o1.cateName)
                    val index1Match = (matchResult?.value) ?: ""
                    val match2Result = regex.find(o2.cateName)
                    val index2Match = (match2Result?.value) ?: ""

                    val index1 = index1Match.toInt()
                    val index2 = index2Match.toInt()

                    // 正确的比较方式
                    return@Comparator index1.compareTo(index2)
                } catch (e: Exception) {
                    // 更健壮的异常处理
                    val o1Valid = isValidVolName(o1.cateName)
                    val o2Valid = isValidVolName(o2.cateName)

                    return@Comparator when {
                        o1Valid && !o2Valid -> 1           // 有效名称排在无效名称之后
                        !o1Valid && o2Valid -> -1          // 无效名称排在有效名称之前
                        else -> o1.cateName.compareTo(o2.cateName)  // 都无效时按名称字符串比较
                    }
                }
                return@Comparator 0
            })
        }
        return nResult.sortedBy { it.id }
    }

    fun querySubCategory(parentId: Int): List<Category> {
        var nResult = listOf<Category>()
        val cursor = query(
            "category",
            columns = arrayOf("id", "cateName"),
            selection = "parentId = ?",
            selectionArgs = arrayOf("$parentId")
        )
        cursor?.use {
            val result = mutableListOf<Category>()
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val name = it.getString(it.getColumnIndexOrThrow("cateName"))

                val category = Category().apply {
                    this.id = id
                    this.cateName = name
                }
                result.add(category)
            }

            nResult = result.sortedWith(Comparator { o1, o2 ->
                try {
                    val regex = Regex("^\\d{1,3}-")  // 注意：Kotlin 字符串中反斜杠需要转义
                    val matchResult = regex.find(o1.cateName)
                    val index1Match = (matchResult?.value) ?: ""
                    val match2Result = regex.find(o2.cateName)
                    val index2Match = (match2Result?.value) ?: ""

                    val index1 = index1Match.toInt()
                    val index2 = index2Match.toInt()

                    // 正确的比较方式
                    return@Comparator index1.compareTo(index2)
                } catch (e: Exception) {
                    // 更健壮的异常处理
                    val o1Valid = isValidVolName(o1.cateName)
                    val o2Valid = isValidVolName(o2.cateName)

                    return@Comparator when {
                        o1Valid && !o2Valid -> 1           // 有效名称排在无效名称之后
                        !o1Valid && o2Valid -> -1          // 无效名称排在有效名称之前
                        else -> o1.cateName.compareTo(o2.cateName)  // 都无效时按名称字符串比较
                    }
                }
                return@Comparator 0
            })
        }
        return nResult.sortedBy { it.id }
    }

    suspend fun queryChapter(
        volumeId: Int,
        onSuccess: (List<Chapter>) -> Unit,
        onFail: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val result = mutableListOf<Chapter>()
                val cursor = query(
                    "chapter",
                    columns = arrayOf("id", "name", "volumeId", "indexId"),
                    //columns = arrayOf("id", "categoryId", "name", "volumeId", "indexId"),
                    selection = "volumeId = ?",
                    selectionArgs = arrayOf("$volumeId")
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getInt(it.getColumnIndexOrThrow("id"))
                        //val categoryId = it.getInt(it.getColumnIndexOrThrow("categoryId"))
                        val name = it.getString(it.getColumnIndexOrThrow("name"))
                        val volumeId = it.getInt(it.getColumnIndexOrThrow("volumeId"))
                        val indexId = it.getInt(it.getColumnIndexOrThrow("indexId"))

                        val chapter = Chapter().apply {
                            this.id = id
                            //this.categoryId = categoryId
                            this.categoryId = -1
                            this.name = name
                            this.volumeId = volumeId
                            this.indexId = indexId
                        }
                        result.add(chapter)
                    }
                }
                withContext(Dispatchers.Main) {
                    onSuccess.invoke(result)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFail.invoke()
                }
            }
        }
    }


    suspend fun queryChapterWithSubCategoryId(subCategoryIds: List<Int>): List<Chapter> {
        var pageNumber = 1
        var pageSize = 40
        val offset1: Int = (pageNumber - 1) * pageSize


        val result = mutableListOf<Chapter>()
        withContext(Dispatchers.IO) {
            try {

                val query = """
        SELECT 
            chapter.id,
            chapter.categoryId,
            chapter.name,
            chapter.volumeId,
            chapter.indexId,
            chapter.content,
            volume.volName
        FROM 
            chapter
        LEFT JOIN 
            volume ON volume.id = chapter.volumeId
        WHERE chapter.categoryId in (${subCategoryIds.joinToString(",")})
    """.trimIndent()

                // 执行查询
                val cursor = database?.rawQuery(query, null)

//                val cursor = query("chapter",
//                    columns = arrayOf("id","categoryId","name","volumeId","indexId","content"),
//                    selection = "categoryId in (${subCategoryIds.joinToString(",")})",
//                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getInt(it.getColumnIndexOrThrow("id"))
                        val categoryId = it.getInt(it.getColumnIndexOrThrow("categoryId"))
                        val name = it.getString(it.getColumnIndexOrThrow("name"))
                        val volumeId = it.getInt(it.getColumnIndexOrThrow("volumeId"))
                        val indexId = it.getInt(it.getColumnIndexOrThrow("indexId"))
                        val content = it.getString(it.getColumnIndexOrThrow("content"))
                        val volName = it.getString(it.getColumnIndexOrThrow("volName"))

                        val chapter = Chapter().apply {
                            this.id = id
                            this.categoryId = categoryId
                            this.name = name
                            this.volumeId = volumeId
                            this.indexId = indexId
                            this.content = content
                            this.volName = volName
                        }
                        result.add(chapter)
                    }
                }
            } catch (e: Exception) {
            }
        }
        return result
    }
    fun isAllEnglish(text: String): Boolean {
        return text.matches("^[a-zA-Z\\x20-\\x7E]+$".toRegex())
    }


    suspend fun querylimitChapterWithSubCategoryIdFindAll(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
    ): List<Chapter> {

        var selectAnd = ""
        searchContentList.forEach { item ->
            //删除最后的空格
            item.trimEnd()
            if(AppSettingUtil.getIsOpenEn()){
                if(isAllEnglish(item)){
                    selectAnd = selectAnd + "and  chapter.content like '%${item}%'"
                }
            }else{
                if(isAllEnglish(item)){
                    //selectAnd = selectAnd + "and  chapter.content like '%${item} %'"
                }else{
                    selectAnd = selectAnd + "and  chapter.content like '%${item}%'"
                }
            }


        }
       // LogUtils.e("查询id==========》》${selectAnd}")

        //val offset = (page - 1) * limit
        val result = mutableListOf<Chapter>()
        withContext(Dispatchers.IO) {
            try {

                val query = """
        SELECT 
            count(*)
        FROM 
            chapter
        LEFT JOIN 
            volume ON volume.id = chapter.volumeId
        WHERE chapter.categoryId in (${subCategoryIds.joinToString(",")})
         $selectAnd
        ORDER BY  chapter.volumeId 
      
    """.trimIndent()
                //ORDER BY chapter.categoryId , chapter.volumeId
                // ORDER BY chapter.volumeId DESC
                LogUtils.e("全局查询id=============>>${query}")

                // 执行查询
                val cursor = database?.rawQuery(query, null)
                if(cursor?.moveToFirst() == true){
                    LogUtils.e("全局查询id=============>>${cursor.getInt(0)}")
                }
                cursor?.close()
                database?.close()
                //LogUtils.e("查询id==========${query}")

//                val cursor = query("chapter",
//                    columns = arrayOf("id","categoryId","name","volumeId","indexId","content"),
//                    selection = "categoryId in (${subCategoryIds.joinToString(",")})",
//                )

            } catch (e: Exception) {
            }
        }
        //Log.d("DatabaseHelper", "page:$page pageSize:$limit")

        //logQueryResult(result, limit, offset)
        LogUtils.e("查询id=============>>${result.size}")
        return result
    }


    suspend fun querylimitChapterWithSubCategoryIdCount(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
    ): Int {

        var selectAnd = ""
        searchContentList.forEach { item ->
           val key =   item.trimEnd()
            //var key = "\\b"+item.trimEnd()+"\\b"
            if(AppSettingUtil.getIsOpenEn()){
                if(isAllEnglish(item)){
                   // selectAnd = selectAnd + "and  paragraph.content like '%${item}%'"
                   // selectAnd = selectAnd + "and  paragraph.content regexp '${item}'"
                    selectAnd = selectAnd + "and  paragraph.content like '%${key}%' COLLATE NOCASE"
                }
            }else{
                if(isAllEnglish(item)){
                    //selectAnd = selectAnd + "and  chapter.content like '%${item} %'"
                }else{
                    selectAnd = selectAnd + "and  paragraph.content like '%${item}%'"
                }
            }


        }

        var count = 0
        withContext(Dispatchers.IO) {
            try {

                val query = """
        SELECT 
            count(content) as total_count
        FROM 
            paragraph
        WHERE paragraph.category_id in (${subCategoryIds.joinToString(",")})
         $selectAnd
       
      
    """.trimIndent()

                LogUtils.e("====================>>${query}")

                // 执行查询
                val cursor = database?.rawQuery(query, null)

                cursor?.use {
                    while (it.moveToNext()) {
                        // LogUtils.e("查询id=============>>${it}")



                         count  = it.getInt(it.getColumnIndexOrThrow("total_count"))

                        //LogUtils.e("查询id=============>>${chapter}")

                    }
                }
                //database?.close()


            } catch (e: Exception) {
            }
        }
        LogUtils.e("查询id002=============>>${count}")
        return count
    }

    suspend fun querylimitChapterWithSubCategoryId1(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
        limit: Int = 20,
        page: Int
    ): List<Chapter> {

        var selectAnd = ""
        searchContentList.forEach { item ->
            // item.trimEnd()
            var key = item.trimEnd()
            //var key = "\\b"+item+"\\b"
            if(AppSettingUtil.getIsOpenEn()){
                if(isAllEnglish(item)){
                    //selectAnd = selectAnd + "and  paragraph.content like '%${item}%'"
                    selectAnd = selectAnd + "and  paragraph.content like '%${key}%' COLLATE NOCASE"
                }
            }else{
                if(isAllEnglish(item)){
                    //selectAnd = selectAnd + "and  chapter.content like '%${item} %'"
                }else{
                    selectAnd = selectAnd + "and  paragraph.content like '%${item}%'"
                }
            }


        }

        val offset = (page) * limit
        //LogUtils.e("offsetid=============>>---->>>${offset}")
        val result = mutableListOf<Chapter>()
        withContext(Dispatchers.IO) {
            try {

                val query = """
        SELECT 
            paragraph.id,
            paragraph.content,
            paragraph.paragraph_index,
            paragraph.volume_id,
            paragraph.chapter_id,
            paragraph.category_id,
            volume.volName,
            chapter.name
         
        FROM 
            paragraph
         LEFT JOIN 
            chapter ON chapter.id = paragraph.chapter_id
          LEFT JOIN 
            volume ON volume.id = paragraph.volume_id
         
        WHERE paragraph.category_id in (${subCategoryIds.joinToString(",")})
         $selectAnd
        ORDER BY  volume_id
       
       LIMIT 20 OFFSET ${offset}
     
        
    """.trimIndent()
               // LIMIT 20 OFFSET ${offset}
                /*${if (limit > 0) "LIMIT $limit" else ""}
                ${if (offset > 0) "OFFSET $offset" else ""}*/
                //ORDER BY chapter.categoryId , chapter.volumeId
                // ORDER BY chapter.volumeId DESC


                // 执行查询
                val cursor = database?.rawQuery(query, null)
                //LogUtils.e("查询id==========${query}")

//                val cursor = query("chapter",
//                    columns = arrayOf("id","categoryId","name","volumeId","indexId","content"),
//                    selection = "categoryId in (${subCategoryIds.joinToString(",")})",
//                )
                cursor?.use {
                    while (it.moveToNext()) {
                       // LogUtils.e("查询id=============>>${it}")



                        val id = it.getInt(it.getColumnIndexOrThrow("chapter_id"))
                        val categoryId = it.getInt(it.getColumnIndexOrThrow("category_id"))
                        val name = it.getString(it.getColumnIndexOrThrow("name"))
                        val volumeId = it.getInt(it.getColumnIndexOrThrow("volume_id"))
                        val indexId = it.getInt(it.getColumnIndexOrThrow("paragraph_index"))
                        val content = it.getString(it.getColumnIndexOrThrow("content"))
                        val volName = it.getString(it.getColumnIndexOrThrow("volName"))
                        //LogUtils.e("scrollIndex============${indexId}")
                        val chapter = Chapter().apply {
                            this.id = id
                            this.categoryId = categoryId
                            this.name = name
                            this.volumeId = volumeId
                            this.indexId = indexId
                            this.content = content
                            this.volName = volName
                        }
                        result.add(chapter)
                        //LogUtils.e("查询id=============>>${chapter}")
                    }
                }
            } catch (e: Exception) {
            }
        }
        //Log.d("DatabaseHelper", "page:$page pageSize:$limit")

        //logQueryResult(result, limit, offset)
        LogUtils.e("页数------${offset}=====列表数据===========>>>>>>${result.size}")
        return result.distinct()
    }


    suspend fun querylimitChapterWithSubCategoryId2(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,

    ): Int {

        var selectAnd = ""
        searchContentList.forEach { item ->
            // item.trimEnd()
            var key = item.trimEnd()
            //var key = "\\b"+item+"\\b"
            if(AppSettingUtil.getIsOpenEn()){
                if(isAllEnglish(item)){
                    //selectAnd = selectAnd + "and  paragraph.content like '%${item}%'"
                    selectAnd = selectAnd + "and  paragraph.content like '%${key}%' COLLATE NOCASE"
                }
            }else{
                if(isAllEnglish(item)){
                    //selectAnd = selectAnd + "and  chapter.content like '%${item} %'"
                }else{
                    selectAnd = selectAnd + "and  paragraph.content like '%${item}%'"
                }
            }


        }


        val result = 0
        withContext(Dispatchers.IO) {
            try {

                val query = """
        SELECT 
           COUNT(p.id) AS total_count
         
        FROM 
            paragraph p
         LEFT JOIN 
            chapter ON chapter.id = paragraph.chapter_id
          LEFT JOIN 
            volume ON volume.id = paragraph.volume_id
         
        WHERE paragraph.category_id in (${subCategoryIds.joinToString(",")})
         $selectAnd
        
       
       
     
        
    """.trimIndent()
                // LIMIT 20 OFFSET ${offset}
                /*${if (limit > 0) "LIMIT $limit" else ""}
                ${if (offset > 0) "OFFSET $offset" else ""}*/
                //ORDER BY chapter.categoryId , chapter.volumeId
                // ORDER BY chapter.volumeId DESC
                LogUtils.e("查询id=============>>${query}")

                // 执行查询
                val cursor = database?.rawQuery(query, null)
                //LogUtils.e("查询id==========${query}")

//                val cursor = query("chapter",
//                    columns = arrayOf("id","categoryId","name","volumeId","indexId","content"),
//                    selection = "categoryId in (${subCategoryIds.joinToString(",")})",
//                )
                cursor?.use {
                    while (it.moveToNext()) {
                        // LogUtils.e("查询id=============>>${it}")



                        val result  = it.getInt(it.getColumnIndexOrThrow("total_count"))

                        //LogUtils.e("查询id=============>>${chapter}")
                        LogUtils.e("查询id=============>>${result}")
                    }
                }
            } catch (e: Exception) {
            }
        }
        //Log.d("DatabaseHelper", "page:$page pageSize:$limit")

        //logQueryResult(result, limit, offset)

        return result;
    }



    suspend fun querylimitChapterWithSubCategoryId(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
        limit: Int = -1,
        page: Int
    ): List<Chapter> {

        var selectAnd = ""
        searchContentList.forEach { item ->

            if(AppSettingUtil.getIsOpenEn()){
                if(isAllEnglish(item)){
                    selectAnd = selectAnd + "and  chapter.content like '%${item}%'"
                }
            }else{
                if(isAllEnglish(item)){
                    //selectAnd = selectAnd + "and  chapter.content like '%${item} %'"
                }else{
                    selectAnd = selectAnd + "and  chapter.content like '%${item}%'"
                }
            }
            //LogUtils.e("查询id==========${selectAnd}")
            /*if(isAllEnglish(item)){
                selectAnd = selectAnd + "and  chapter.content like '%${item} %'"
            }else{
                selectAnd = selectAnd + "and  chapter.content like '%${item}%'"
            }*/

        }
        LogUtils.e("查询id==========》》${selectAnd}")

        val offset = (page - 1) * limit
        val result = mutableListOf<Chapter>()
        withContext(Dispatchers.IO) {
            try {

                val query = """
        SELECT 
            chapter.id,
            chapter.categoryId,
            chapter.name,
            chapter.volumeId,
            chapter.indexId,
            chapter.content,
            volume.volName
        FROM 
            chapter
        LEFT JOIN 
            volume ON volume.id = chapter.volumeId
        WHERE chapter.categoryId in (${subCategoryIds.joinToString(",")})
         $selectAnd
        ORDER BY  chapter.volumeId 
       
      
        ${if (limit > 0) "LIMIT $limit" else ""}
        ${if (offset > 0) "OFFSET $offset" else ""}
    """.trimIndent()
                //ORDER BY chapter.categoryId , chapter.volumeId
                // ORDER BY chapter.volumeId DESC
                LogUtils.e("查询id=============>>${query}")

                // 执行查询
                val cursor = database?.rawQuery(query, null)
                //LogUtils.e("查询id==========${query}")

//                val cursor = query("chapter",
//                    columns = arrayOf("id","categoryId","name","volumeId","indexId","content"),
//                    selection = "categoryId in (${subCategoryIds.joinToString(",")})",
//                )
                cursor?.use {
                    while (it.moveToNext()) {



                        val id = it.getInt(it.getColumnIndexOrThrow("id"))
                        val categoryId = it.getInt(it.getColumnIndexOrThrow("categoryId"))
                        val name = it.getString(it.getColumnIndexOrThrow("name"))
                        val volumeId = it.getInt(it.getColumnIndexOrThrow("volumeId"))
                        val indexId = it.getInt(it.getColumnIndexOrThrow("indexId"))
                        val content = it.getString(it.getColumnIndexOrThrow("content"))
                        val volName = it.getString(it.getColumnIndexOrThrow("volName"))

                        val chapter = Chapter().apply {
                            this.id = id
                            this.categoryId = categoryId
                            this.name = name
                            this.volumeId = volumeId
                            this.indexId = indexId
                            this.content = content
                            this.volName = volName
                        }
                        result.add(chapter)

                    }
                }
            } catch (e: Exception) {
            }
        }
        Log.d("DatabaseHelper", "page:$page pageSize:$limit")

        //logQueryResult(result, limit, offset)
        LogUtils.e("查询id==========${result.size}")
        return result
    }


    private fun logQueryResult(result: List<Chapter>, limit: Int, offset: Int) {
        try {
            // 使用 Gson 格式化输出
            val jsonStr = GsonUtils.toJsonPretty(result)

            // 打印日志
            val logTag = "DatabaseHelper"
            val logMessage = "pageSize:$limit  page:$offset \n$jsonStr"

            // 分段处理长日志
            if (logMessage.length > 4000) {
                Log.d(logTag, "查询结果 (${result.size} 条记录):")
                result.forEachIndexed { index, chapter ->
                    Log.d(logTag, "${index + 1}. ID: ${chapter.id}, Name: ${chapter.name}")
                }
            } else {
                Log.d(logTag, logMessage)
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "日志转换失败: ${e.message}")
            Log.d("DatabaseHelper", "查询结果: ${result.size} 条记录")
        }
    }

    suspend fun queryChapterWithParentId(
        categoryId: Int,
        onSuccess: (List<Chapter>) -> Unit,
        onFail: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val result = mutableListOf<Chapter>()
                val cursor = query(
                    "chapter",
                    columns = arrayOf("id", "categoryId", "name", "volumeId", "indexId", "content"),
                    selection = "parentId = ?",
                    selectionArgs = arrayOf("$categoryId")
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getInt(it.getColumnIndexOrThrow("id"))
                        val categoryId = it.getInt(it.getColumnIndexOrThrow("categoryId"))
                        val name = it.getString(it.getColumnIndexOrThrow("name"))
                        val volumeId = it.getInt(it.getColumnIndexOrThrow("volumeId"))
                        val indexId = it.getInt(it.getColumnIndexOrThrow("indexId"))
                        val content = it.getString(it.getColumnIndexOrThrow("content"))

                        val chapter = Chapter().apply {
                            this.id = id
                            this.categoryId = categoryId
                            this.name = name
                            this.volumeId = volumeId
                            this.indexId = indexId
                            this.content = content
                        }
                        result.add(chapter)
                    }
                }
                withContext(Dispatchers.Main) {
                    onSuccess.invoke(result)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFail.invoke()
                }
            }
        }
    }

    suspend fun queryVolume(
        subCategoryIds: List<Int>,
        onSuccess: (List<Volume>) -> Unit,
        onFail: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                var nResult = listOf<Volume>()
                val cursor = query(
                    "volume",
                    columns = arrayOf("id", "categoryId", "volName"),
                    selection = "categoryId in (${subCategoryIds.joinToString(",")})"
                )
                cursor?.use {
                    val result = mutableListOf<Volume>()
                    while (it.moveToNext()) {
                        val id = it.getInt(it.getColumnIndexOrThrow("id"))
                        val categoryId = it.getInt(it.getColumnIndexOrThrow("categoryId"))
                        val volName = it.getString(it.getColumnIndexOrThrow("volName"))

                        val chapter = Volume().apply {
                            this.id = id
                            this.volName = volName
                            this.categoryId = categoryId
                        }
                        result.add(chapter)
                    }


                    nResult = result.sortedWith(Comparator { o1, o2 ->
                        try {

                            val regex = Regex("^\\d{1,3}-")  // 注意：Kotlin 字符串中反斜杠需要转义
                            val matchResult = regex.find(o1.volName)
                            val index1Match = (matchResult?.value) ?: ""
                            val match2Result = regex.find(o2.volName)
                            val index2Match = (match2Result?.value) ?: ""

                            val index1 = index1Match.toInt()
                            val index2 = index2Match.toInt()

                            // 正确的比较方式
                            return@Comparator index1.compareTo(index2)
                        } catch (e: Exception) {
                            // 更健壮的异常处理
                            val o1Valid = isValidVolName(o1.volName)
                            val o2Valid = isValidVolName(o2.volName)

                            return@Comparator when {
                                o1Valid && !o2Valid -> 1           // 有效名称排在无效名称之后
                                !o1Valid && o2Valid -> -1          // 无效名称排在有效名称之前
                                else -> o1.volName.compareTo(o2.volName)  // 都无效时按名称字符串比较
                            }
                        }
                    })
                }

                withContext(Dispatchers.Main) {
                    onSuccess.invoke(nResult.sortedBy { it.id })
                }

            } catch (e: Exception) {
                Log.i("ccccccccc", "e=====${e.message}")
                withContext(Dispatchers.Main) {
                    onFail.invoke()
                }
            }
        }
    }

    // 辅助方法：检查volName是否符合预期格式
    private fun isValidVolName(volName: String): Boolean {
        return try {
            val regex = Regex("^\\d{1,3}-")  // 注意：Kotlin 字符串中反斜杠需要转义
            val matchResult = regex.find(volName)
            val index1Match = (matchResult?.value) ?: ""
            index1Match.toInt()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun queryItemChapterContent(chapterId : Int,
                                        matchContent:ArrayList<String>,
                                        onSuccess: (String, String) -> Unit,
                                        onFail: () -> Unit){
        withContext(Dispatchers.IO){
            try {
                var strDetail = ""
                var str = ""
                val cursor = query(
                    "paragraph",
                    columns = arrayOf("content","MusicLinks"),
                    selection = "chapter_id = ?",
                    selectionArgs = arrayOf("$chapterId")
                )
               // LogUtils.e("===========${chapterId}")

                cursor?.use {
                    while (it.moveToNext()) {
                        //LogUtils.e("===========${it}")

                        var content = it.getString(it.getColumnIndexOrThrow("content"))
                        val MusicLinks = it.getString(it.getColumnIndexOrThrow("MusicLinks"))
                        if(!StringUtils.isEmpty(MusicLinks)){
                            str = MusicLinks
                        }
                        strDetail = "${strDetail}\n${content}"


                    }
                    LogUtils.e("=========${strDetail}")
                    LogUtils.e("001=========${str}")
                    withContext(Dispatchers.Main) {

                        onSuccess.invoke(strDetail,str)
                    }
                    return@use strDetail
                }

            }catch (e: Exception){
                e.printStackTrace();
            }
        }
    }

    suspend fun queryChapterContent(
        chapterId: Int,
        onSuccess: (String, String) -> Unit,
        onFail: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val cursor = query(
                    "chapter",
                    columns = arrayOf("content", "MusicLinks"),
                    selection = "id = ?",
                    selectionArgs = arrayOf("$chapterId")
                )

                cursor?.use {
                    while (it.moveToNext()) {

                        val content = it.getString(it.getColumnIndexOrThrow("content"))
                        val MusicLinks = it.getString(it.getColumnIndexOrThrow("MusicLinks"))

                        Log.i("ccccccccc", "MusicLinks====$MusicLinks")
                        withContext(Dispatchers.Main) {
                            onSuccess.invoke(content, MusicLinks)
                        }
                        return@use content
                    }
                }
            } catch (e: Exception) {
                Log.i("cccccccc", "e=====${e.message}")
                withContext(Dispatchers.Main) {
                    onFail.invoke()
                }
            }
        }
    }

    fun String.isAllEnglishAndSymbols(): Boolean {
        // 正则表达式说明：
        // ^ 表示字符串开头
        // $ 表示字符串结尾
        // [a-zA-Z\\x20-\\x7E]+ 表示匹配大小写字母及ASCII码32-126的可打印符号
        // + 表示至少包含一个字符（空字符串返回false）
        return matches(Regex("^[a-zA-Z\\x20-\\x7E]+$"))
    }

    /**
     * 执行查询并返回结果
     */
    fun query(
        table: String,
        columns: Array<String> = emptyArray(),
        selection: String = "",
        selectionArgs: Array<String>? = emptyArray(),
        groupBy: String? = null,
        having: String? = null,
        orderBy: String? = null
    ): Cursor? {
        if (database?.isOpen != true) {
            return null
        }
        return database?.query(table, columns, selection, selectionArgs, groupBy, having, orderBy)
    }

    /**
     * 执行自定义 SQL 查询
     */
    fun rawQuery(sql: String, selectionArgs: Array<String>?): Cursor? {
        if (database?.isOpen != true) {
            return null
        }
        return database?.rawQuery(sql, selectionArgs)
    }
}