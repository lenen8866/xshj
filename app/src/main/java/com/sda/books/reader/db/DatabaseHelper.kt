package com.sda.books.reader.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
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
        const val DB_NAME = "xshj.db"
        private const val BUFFER_SIZE_LARGE = 20480
        private const val BUFFER_SIZE_SMALL = 10240
        private const val INDEX_REGEX_PATTERN = "^\\d{1,3}-"
        
        private var instance: DatabaseHelper? = null
        private var customDbPath: String? = null
        var isFinish = false

        fun getInstance(): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(App.context)
            }
            return instance!!
        }

        fun setDatabasePath(path: String) {
            customDbPath = path
            instance = null
        }
    }

    private var database: SQLiteDatabase? = null
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
        if (dbFile.exists() && dbFile.length() > 0) {
            return@withContext
        }
        
        val dbDir = dbFile.parentFile
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs()
        }

        try {
            context.assets.open(DB_NAME).use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE_LARGE)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                }
            }
        } catch (e: IOException) {
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
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun checkDatabaseExists(): Boolean {
        return File(dbPath).exists()
    }

    @Throws(IOException::class)
    private fun copyDatabase() {
        context.assets.open(DB_NAME).use { inputStream ->
            FileOutputStream(dbPath).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE_SMALL)
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
                copyDatabaseFromAssets()
                database = SQLiteDatabase.openDatabase(
                    dbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                database?.let {
                    Log.d(TAG, "数据库已打开")
                }
                database ?: throw IllegalStateException("数据库打开失败")
            } catch (e: Exception) {
                Log.e(TAG, "打开数据库失败: ${e.message}", e)
                throw e
            }
        }
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

                    if(!name.contains("/")){
                        val category = Category().apply {
                            this.id = id
                            this.cateName = name
                        }
                        result.add(category)
                    }

                }
                nResult = result.sortedWith(createNameComparator { it.cateName })
            }
        }
        return nResult.sortedBy { it.id }
    }

    /**
     * 根据 categoryId 查询分类信息
     */
    suspend fun queryCategoryById(categoryId: Int): Category? {
        return withContext(Dispatchers.IO) {
            try {
                val cursor = query(
                    "category",
                    columns = arrayOf("id", "cateName", "parentId"),
                    selection = "id = ?",
                    selectionArgs = arrayOf("$categoryId")
                )
                cursor?.use {
                    if (it.moveToNext()) {
                        val id = it.getInt(it.getColumnIndexOrThrow("id"))
                        val name = it.getString(it.getColumnIndexOrThrow("cateName"))
                        return@withContext Category().apply {
                            this.id = id
                            this.cateName = name
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "查询分类失败: ${e.message}", e)
                null
            }
        }
    }

    /**
     * 根据章节ID查询章节的categoryId
     */
    suspend fun queryChapterCategoryId(chapterId: Int): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val cursor = query(
                    "chapter",
                    columns = arrayOf("categoryId"),
                    selection = "id = ?",
                    selectionArgs = arrayOf("$chapterId")
                )
                cursor?.use {
                    if (it.moveToNext()) {
                        val categoryIdIndex = it.getColumnIndex("categoryId")
                        if (categoryIdIndex >= 0 && !it.isNull(categoryIdIndex)) {
                            return@withContext it.getInt(categoryIdIndex)
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "查询章节分类ID失败: ${e.message}", e)
                null
            }
        }
    }

    /**
     * 根据章节ID查询分类路径（优先使用parentId，返回格式：父分类-子分类）
     * @param chapterId 章节ID
     * @return 分类路径字符串，格式：父分类-子分类，如果查询失败返回null
     */
    suspend fun queryCategoryPathByChapterId(chapterId: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 查询章节的categoryId
                val categoryId = queryChapterCategoryId(chapterId)
                if (categoryId == null || categoryId <= 0) {
                    Log.e(TAG, "查询章节分类路径失败: chapterId=$chapterId, categoryId为空或无效")
                    return@withContext null
                }
                
                Log.d(TAG, "查询分类路径: chapterId=$chapterId, categoryId=$categoryId")
                
                // 2. 查询分类信息（包含parentId）
                val cursor = query(
                    "category",
                    columns = arrayOf("id", "cateName", "parentId"),
                    selection = "id = ?",
                    selectionArgs = arrayOf("$categoryId")
                )
                
                var categoryName: String? = null
                var parentId: Int? = null
                
                cursor?.use {
                    if (it.moveToNext()) {
                        categoryName = it.getString(it.getColumnIndexOrThrow("cateName"))
                        val parentIdIndex = it.getColumnIndex("parentId")
                        if (parentIdIndex >= 0 && !it.isNull(parentIdIndex)) {
                            parentId = it.getInt(parentIdIndex)
                        }
                    }
                }
                
                if (categoryName.isNullOrEmpty()) {
                    Log.e(TAG, "查询分类路径失败: categoryId=$categoryId, 分类名称为空")
                    return@withContext null
                }
                
                Log.d(TAG, "查询分类路径: categoryId=$categoryId, categoryName=$categoryName, parentId=$parentId")
                
                // 3. 优先使用parentId查询父分类（最可靠的方式）
                if (parentId != null && parentId > 0) {
                    val parentCursor = query(
                        "category",
                        columns = arrayOf("id", "cateName"),
                        selection = "id = ?",
                        selectionArgs = arrayOf("$parentId")
                    )
                    parentCursor?.use {
                        if (it.moveToNext()) {
                            val parentCateName = it.getString(it.getColumnIndexOrThrow("cateName"))
                            Log.d(TAG, "查询分类路径: 通过parentId查询到父分类, parentId=$parentId, parentCateName=$parentCateName")
                            return@withContext "$parentCateName-$categoryName"
                        }
                    }
                }
                
                // 4. Fallback：如果parentId无效，检查分类名称是否包含"/"
                if (categoryName.contains("/")) {
                    val parts = categoryName.split("/", limit = 2)
                    if (parts.size == 2) {
                        Log.d(TAG, "查询分类路径: 通过字符串分割, parentName=${parts[0]}, childName=${parts[1]}")
                        return@withContext "${parts[0]}-${parts[1]}"
                    }
                }
                
                // 5. 如果都没有，只返回当前分类名称
                Log.d(TAG, "查询分类路径: 只返回当前分类名称, categoryName=$categoryName")
                return@withContext categoryName
                
            } catch (e: Exception) {
                Log.e(TAG, "查询分类路径失败: chapterId=$chapterId, error=${e.message}", e)
                null
            }
        }
    }

    /**
     * 查询父分类信息
     */
    suspend fun queryCategoryParent(categoryId: Int): Category? {
        return withContext(Dispatchers.IO) {
            try {
                // 先查询当前分类的 parentId
                val cursor = query(
                    "category",
                    columns = arrayOf("id", "cateName", "parentId"),
                    selection = "id = ?",
                    selectionArgs = arrayOf("$categoryId")
                )
                var parentId: Int? = null
                cursor?.use {
                    if (it.moveToNext()) {
                        val parentIdIndex = it.getColumnIndex("parentId")
                        if (parentIdIndex >= 0 && !it.isNull(parentIdIndex)) {
                            parentId = it.getInt(parentIdIndex)
                        }
                    }
                }
                
                // 如果 parentId 存在且不为 0，查询父分类
                if (parentId != null && parentId > 0) {
                    val parentCursor = query(
                        "category",
                        columns = arrayOf("id", "cateName"),
                        selection = "id = ?",
                        selectionArgs = arrayOf("$parentId")
                    )
                    parentCursor?.use {
                        if (it.moveToNext()) {
                            val id = it.getInt(it.getColumnIndexOrThrow("id"))
                            val name = it.getString(it.getColumnIndexOrThrow("cateName"))
                            return@withContext Category().apply {
                                this.id = id
                                this.cateName = name
                            }
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "查询父分类失败: ${e.message}", e)
                null
            }
        }
    }

    fun querySubCategory1(parentName: String): List<Category> {
        var nResult = listOf<Category>()
        val cursor = query(
            "category",
            columns = arrayOf("id", "cateName")
        )
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

            nResult = result.sortedWith(createNameComparator { it.cateName })
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

            nResult = result.sortedWith(createNameComparator { it.cateName })
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

            nResult = result.sortedWith(createNameComparator { it.cateName })
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
                    selection = "volumeId = ?",
                    selectionArgs = arrayOf("$volumeId")
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getInt(it.getColumnIndexOrThrow("id"))
                        val name = it.getString(it.getColumnIndexOrThrow("name"))
                        val volumeId = it.getInt(it.getColumnIndexOrThrow("volumeId"))
                        val indexId = it.getInt(it.getColumnIndexOrThrow("indexId"))

                        val chapter = Chapter().apply {
                            this.id = id
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

                val cursor = rawQuery(query, null)
                cursor?.use {
                    while (it.moveToNext()) {
                        result.add(createChapterFromCursor(it))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询章节失败: ${e.message}", e)
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
        val selectAnd = buildSearchConditions(searchContentList, useParagraph = false)
        val result = mutableListOf<Chapter>()
        
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    SELECT count(*)
                    FROM chapter
                    LEFT JOIN volume ON volume.id = chapter.volumeId
                    WHERE chapter.categoryId in (${subCategoryIds.joinToString(",")})
                    $selectAnd
                    ORDER BY chapter.volumeId
                """.trimIndent()

                val cursor = database?.rawQuery(query, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        // 此方法仅用于计数，不返回结果
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询失败: ${e.message}", e)
            }
        }
        return result
    }


    suspend fun querylimitChapterWithSubCategoryIdCount(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
    ): Int {
        val selectAnd = buildSearchConditions(searchContentList, useParagraph = true)
        var count = 0
        
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    SELECT count(content) as total_count
                    FROM paragraph
                    WHERE paragraph.category_id in (${subCategoryIds.joinToString(",")})
                    $selectAnd
                """.trimIndent()

                val cursor = rawQuery(query, null)
                cursor?.use {
                    if (it.moveToNext()) {
                        count = it.getInt(it.getColumnIndexOrThrow("total_count"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询计数失败: ${e.message}", e)
            }
        }
        return count
    }

    suspend fun querylimitChapterWithSubCategoryId1(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
        limit: Int = 20,
        page: Int
    ): List<Chapter> {
        val selectAnd = buildSearchConditions(searchContentList, useParagraph = true)
        val offset = page * limit
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
                    FROM paragraph
                    LEFT JOIN chapter ON chapter.id = paragraph.chapter_id
                    LEFT JOIN volume ON volume.id = paragraph.volume_id
                    WHERE paragraph.category_id in (${subCategoryIds.joinToString(",")})
                    $selectAnd
                    ORDER BY volume_id
                    LIMIT $limit OFFSET $offset
                """.trimIndent()

                val cursor = rawQuery(query, null)
                cursor?.use {
                    while (it.moveToNext()) {
                        result.add(createChapterFromParagraphCursor(it))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询章节列表失败: ${e.message}", e)
            }
        }
        return result.distinct()
    }


    suspend fun querylimitChapterWithSubCategoryId2(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
    ): Int {
        val selectAnd = buildSearchConditions(searchContentList, useParagraph = true)
        var result = 0
        
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    SELECT COUNT(p.id) AS total_count
                    FROM paragraph p
                    LEFT JOIN chapter ON chapter.id = p.chapter_id
                    LEFT JOIN volume ON volume.id = p.volume_id
                    WHERE p.category_id in (${subCategoryIds.joinToString(",")})
                    $selectAnd
                """.trimIndent()

                val cursor = database?.rawQuery(query, null)
                cursor?.use {
                    if (it.moveToNext()) {
                        result = it.getInt(it.getColumnIndexOrThrow("total_count"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询计数失败: ${e.message}", e)
            }
        }
        return result
    }



    suspend fun querylimitChapterWithSubCategoryId(
        subCategoryIds: List<Int>,
        searchContentList: List<String>,
        limit: Int = -1,
        page: Int
    ): List<Chapter> {
        val selectAnd = buildSearchConditions(searchContentList, useParagraph = false)
        val offset = if (limit > 0) (page - 1) * limit else 0
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
                    FROM chapter
                    LEFT JOIN volume ON volume.id = chapter.volumeId
                    WHERE chapter.categoryId in (${subCategoryIds.joinToString(",")})
                    $selectAnd
                    ORDER BY chapter.volumeId
                    ${if (limit > 0) "LIMIT $limit" else ""}
                    ${if (offset > 0) "OFFSET $offset" else ""}
                """.trimIndent()

                val cursor = rawQuery(query, null)
                cursor?.use {
                    while (it.moveToNext()) {
                        result.add(createChapterFromCursor(it))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询章节列表失败: ${e.message}", e)
            }
        }
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


                    nResult = result.sortedWith(createNameComparator { it.volName })
                }

                withContext(Dispatchers.Main) {
                    onSuccess.invoke(nResult.sortedBy { it.id })
                }

            } catch (e: Exception) {
                Log.e(TAG, "查询卷列表失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onFail.invoke()
                }
            }
        }
    }

    /**
     * 提取索引数字用于排序
     */
    private fun extractIndex(name: String): Int? {
        return try {
            val regex = Regex(INDEX_REGEX_PATTERN)
            val matchResult = regex.find(name)
            matchResult?.value?.replace("-", "")?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查名称是否符合预期格式
     */
    private fun isValidVolName(volName: String): Boolean {
        return extractIndex(volName) != null
    }

    /**
     * 创建名称排序比较器
     */
    private fun <T> createNameComparator(getName: (T) -> String): Comparator<T> {
        return Comparator { o1, o2 ->
            try {
                val index1 = extractIndex(getName(o1)) ?: return@Comparator 0
                val index2 = extractIndex(getName(o2)) ?: return@Comparator 0
                index1.compareTo(index2)
            } catch (e: Exception) {
                val o1Valid = isValidVolName(getName(o1))
                val o2Valid = isValidVolName(getName(o2))
                when {
                    o1Valid && !o2Valid -> 1
                    !o1Valid && o2Valid -> -1
                    else -> getName(o1).compareTo(getName(o2))
                }
            }
        }
    }

    /**
     * 从 Cursor 创建 Chapter 对象（包含 volName）
     */
    private fun createChapterFromCursor(cursor: Cursor): Chapter {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
        val categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("categoryId"))
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        val volumeId = cursor.getInt(cursor.getColumnIndexOrThrow("volumeId"))
        val indexId = cursor.getInt(cursor.getColumnIndexOrThrow("indexId"))
        val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
        val volName = cursor.getString(cursor.getColumnIndexOrThrow("volName"))
        
        return Chapter().apply {
            this.id = id
            this.categoryId = categoryId
            this.name = name
            this.volumeId = volumeId
            this.indexId = indexId
            this.content = content
            this.volName = volName
        }
    }

    /**
     * 从 Cursor 创建 Chapter 对象（paragraph 表，包含 paragraph_index）
     */
    private fun createChapterFromParagraphCursor(cursor: Cursor): Chapter {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow("chapter_id"))
        val categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"))
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        val volumeId = cursor.getInt(cursor.getColumnIndexOrThrow("volume_id"))
        val indexId = cursor.getInt(cursor.getColumnIndexOrThrow("paragraph_index"))
        val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
        val volName = cursor.getString(cursor.getColumnIndexOrThrow("volName"))
        
        return Chapter().apply {
            this.id = id
            this.categoryId = categoryId
            this.name = name
            this.volumeId = volumeId
            this.indexId = indexId
            this.content = content
            this.volName = volName
        }
    }

    /**
     * 构建搜索查询条件
     */
    private fun buildSearchConditions(searchContentList: List<String>, useParagraph: Boolean = false): String {
        val tableColumn = if (useParagraph) "paragraph.content" else "chapter.content"
        val selectAnd = StringBuilder()
        
        searchContentList.forEach { item ->
            val key = item.trimEnd()
            val isEnglish = isAllEnglish(item)
            val isOpenEn = AppSettingUtil.getIsOpenEn()
            
            when {
                isOpenEn && isEnglish -> {
                    selectAnd.append(" and $tableColumn like '%$key%' COLLATE NOCASE")
                }
                !isOpenEn && !isEnglish -> {
                    selectAnd.append(" and $tableColumn like '%$item%'")
                }
            }
        }
        
        return selectAnd.toString()
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
                        val content = it.getString(it.getColumnIndexOrThrow("content"))
                        val MusicLinks = it.getString(it.getColumnIndexOrThrow("MusicLinks"))
                        if(!StringUtils.isEmpty(MusicLinks)){
                            str = MusicLinks
                        }
                        strDetail = "${strDetail}\n${content}"
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess.invoke(strDetail, str)
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        onFail.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询章节内容失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onFail.invoke()
                }
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

                        Log.d(TAG, "MusicLinks: $MusicLinks")
                        withContext(Dispatchers.Main) {
                            onSuccess.invoke(content, MusicLinks)
                        }
                        return@withContext
                    }
                }
                // 如果没有找到数据，调用失败回调
                withContext(Dispatchers.Main) {
                    onFail.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "查询章节内容失败: ${e.message}", e)
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
     * 检查数据库是否已打开
     */
    private fun ensureDatabaseOpen(): Boolean {
        return database?.isOpen == true
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
        if (!ensureDatabaseOpen()) {
            Log.w(TAG, "数据库未打开，无法执行查询")
            return null
        }
        return database?.query(table, columns, selection, selectionArgs, groupBy, having, orderBy)
    }

    /**
     * 执行自定义 SQL 查询
     */
    fun rawQuery(sql: String, selectionArgs: Array<String>?): Cursor? {
        if (!ensureDatabaseOpen()) {
            Log.w(TAG, "数据库未打开，无法执行原始查询")
            return null
        }
        return database?.rawQuery(sql, selectionArgs)
    }
}