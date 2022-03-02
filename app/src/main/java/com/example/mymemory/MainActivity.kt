package com.example.mymemory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryGame
import com.example.mymemory.models.UserImageList
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.example.mymemory.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {


    companion object{
        private const val TAG = "MainActivity"
        private  const val  CREATE_REQUEST_CODE = 248

    }
    private val db = Firebase.firestore
    private var gameName: String? = null
    private  var customGameImages: List<String>? = null
    private lateinit var memoryGame: MemoryGame
    private lateinit var clRoot: CoordinatorLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private  lateinit var adapter: MemoryBoardAdapter

    private var boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves =findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)




        setupBoard()
      /*  tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))

        memoryGame = MemoryGame(boardSize)


        adapter= MemoryBoardAdapter(this, boardSize, memoryGame.cards,object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })
        rvBoard.adapter =adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager =GridLayoutManager(this, boardSize.getWidth())*/

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
         when (item.itemId){
             R.id.mi_refresh ->{
                 if(memoryGame .getNumMoves() > 0 && !memoryGame.haveWonGame()){
                     showAlertDialog("Quit current game?" , null, View.OnClickListener {
                         setupBoard()
                     })
                 }else {

                     setupBoard()
                 }
                 return true

             }
             R.id.mi_new_size->{
                 showNewSizeDialog()
                 return true

             }
             R.id.mi_custom ->{
                 showCreationDialog()
                 return true
             }
             R.id.mi_download->{
                 showDownloadDialog()
                 return true


             }

         }
        return super.onOptionsItemSelected(item)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName =  data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null){
                Log.e(TAG, "Got null Custom game from Crate Activity")
                return
            }
            downloadGame(customGameName)

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_dowload_board, null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)

        })

    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener {document ->
            val userImageList :UserImageList? = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null){
                Log.e(TAG, "invalid custom game data from firestore")
                Snackbar.make(clRoot, "Sorry we couldn't find any such game, '$customGameName'", Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val numCards:Int = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            for(imageUrl in userImageList.images){

                Picasso.get().load(imageUrl).placeholder(R.drawable.ic_image).fetch()
            }
            Snackbar.make(clRoot ,"You're playing the custom game $customGameName", Snackbar.LENGTH_LONG).show()
            gameName = customGameName
            setupBoard()

        }.addOnFailureListener{exception ->
            Log.e(TAG,"Exception while retriveing game",  exception)
        }

    }

    private fun showCreationDialog() {
        val boardSizeView:View =  LayoutInflater.from(this) .inflate(R.layout.dialog_board_size, null)
        val radioGroupSize  = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Crate your own memory board" , boardSizeView, View.OnClickListener {
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD

            }

            //Navigate to a new activity
            val intent = Intent(this,CreateActivity::class.java )
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)

        })

    }

    private fun showNewSizeDialog() {
        val boardSizeView:View =  LayoutInflater.from(this) .inflate(R.layout.dialog_board_size, null)
        val radioGroupSize  = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM ->radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD ->radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size" , boardSizeView, View.OnClickListener {
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()


        })


    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListner: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel" , null)
            .setPositiveButton("OK"){ _,_ ->
                positiveClickListner.onClick(null)

            }.show()
    }

    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)

        when(boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"

            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"

            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6  x 4"
                tvNumPairs.text = "Pairs: 0 / 12"

            }
        }

        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize, customGameImages)
        adapter= MemoryBoardAdapter(this, boardSize, memoryGame.cards,object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })
        rvBoard.adapter =adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager =GridLayoutManager(this, boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {

        if(memoryGame.haveWonGame()){
            Snackbar.make(clRoot, "You already won", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){
            Snackbar.make(clRoot, "invalid Move", Snackbar.LENGTH_SHORT).show()
            return


        }

        if(memoryGame.flipCard(position)){
            Log.i(TAG,"Found a match!!, Num Pairs Found: ${memoryGame.numPairsFound}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat()/ boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)

            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text ="Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"

            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot, "You won!", Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.RED, Color.YELLOW, Color.CYAN)).oneShot()
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()} "
        adapter.notifyDataSetChanged()

    }
}