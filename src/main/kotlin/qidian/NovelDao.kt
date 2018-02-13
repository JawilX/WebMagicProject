package qidian

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

class NovelDao {

    private lateinit var connection: Connection
    private lateinit var insertStatement: PreparedStatement
    private lateinit var updateStatement: PreparedStatement
    private lateinit var selectUrlAndScoreStatement: PreparedStatement

    init {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance()
            val url = "jdbc:mysql://localhost:3306/book?useSSL=false&user=root&password=1231&useUnicode=true&characterEncoding=utf8"
            connection = DriverManager.getConnection(url)
            val insert = "INSERT INTO novel (name, author, word_count, score, score_count, url, intro, create_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW());"
            insertStatement = connection.prepareStatement(insert)
            val update = "UPDATE novel SET score = ?, score_count = ?, update_at = NOW() WHERE name = ? AND author = ?;"
            updateStatement = connection.prepareStatement(update)
            val selectUrlAndScore = "SELECT score, url FROM novel;"
            selectUrlAndScoreStatement = connection.prepareStatement(selectUrlAndScore)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insert(novel: Novel): Int {
        try {
            insertStatement.setString(1, novel.name)
            insertStatement.setString(2, novel.author)
            insertStatement.setString(3, novel.wordCount)
            insertStatement.setString(4, novel.score)
            insertStatement.setString(5, novel.scoreCount)
            insertStatement.setString(6, novel.url)
            insertStatement.setString(7, novel.intro)
            insertStatement.executeUpdate()
        } catch (e: Exception) {
//            e.printStackTrace()
        }
        return -1
    }

    fun update(novel: Novel): Int {
        try {
            if (novel.score != "0.0" && novel.scoreCount != "0") {
                updateStatement.setString(1, novel.score)
                updateStatement.setString(2, novel.scoreCount)
                updateStatement.setString(3, novel.name)
                updateStatement.setString(4, novel.author)
                return updateStatement.executeUpdate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1
    }

    fun selectUrlAndScore(): HashMap<String, String> {
        val result = HashMap<String, String>()
        try {
            val resultSet = selectUrlAndScoreStatement.executeQuery()
            while (resultSet.next()) {
                result.put(resultSet.getString("url"),
                        resultSet.getString("score"))
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    companion object {
        val instance = NovelDao()
    }

}