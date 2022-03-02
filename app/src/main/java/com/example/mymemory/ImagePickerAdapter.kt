package com.example.mymemory

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
        private val context: Context,
        private val imageUris: List<Uri>,
        private val boardSize: BoardSize,
        private val imageClickListener: ImageClickListener
    ) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "ImagePickerAdapter"
        private const val Margin_Size = 10
    }

    interface ImageClickListener{
        fun onPlaceholderClicked()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        Log.i(TAG, "onCreateViewHolder")
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent , false)
        val cardWidth = parent.width / boardSize.getWidth() - (2 * Margin_Size)
        val cardHeight = parent.height/ boardSize.getHeight()  - (2 * Margin_Size)
        Log.i(TAG, "onCreateViewHolder ${parent.width}")
        val cardSideLength: Int = min(cardWidth, cardHeight)
        Log.i(TAG, "onCreateViewHolder $cardSideLength, $cardWidth, $cardHeight")
        val layoutParams: ViewGroup.LayoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength

        return ViewHolder(view)

    }

    override fun getItemCount() = boardSize.getNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position < imageUris.size){
             holder.bind(imageUris[position])
        }else{
            holder.bind()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val  ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)



        fun bind(){
            ivCustomImage.setOnClickListener{
                imageClickListener.onPlaceholderClicked()


            }

        }

        fun bind(uri :Uri){
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)

        }

    }

}
