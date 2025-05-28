import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.googlemaparea.databinding.ItemRvBinding
import com.example.googlemaparea.model.DistanceEntity
import com.example.googlemaparea.model.PolygonEntity
import com.example.googlemaparea.utils.ListItem

class MixedAdapter(
    val list: List<ListItem>,
    val action: Action
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_POLYGON = 1
        const val TYPE_DISTANCE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is PolygonEntity -> TYPE_POLYGON
            is DistanceEntity -> TYPE_DISTANCE
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_POLYGON -> {
                val binding = ItemRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PolygonVh(binding)
            }
            TYPE_DISTANCE -> {
                val binding = ItemRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DistanceVh(binding)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PolygonVh -> holder.onBind(list[position] as PolygonEntity)
            is DistanceVh -> holder.onBind(list[position] as DistanceEntity)
        }
    }

    inner class PolygonVh(val binding: ItemRvBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(data: PolygonEntity) {
            binding.tvName.text = data.name
            binding.tvNote.text = data.note
            binding.tvArea.text = "Area: %.2f m²".format(data.area)
            binding.root.setOnClickListener { action.onPolygonClick(data) }
        }
    }

    inner class DistanceVh(val binding: ItemRvBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(data: DistanceEntity) {
            binding.tvName.text = data.name
            binding.tvNote.text = data.note
            binding.tvArea.text = "Distance: %.2f m".format(data.totalDistance)
            binding.root.setOnClickListener { action.onDistanceClick(data) }
        }
    }

    interface Action {
        fun onPolygonClick(polygon: PolygonEntity)
        fun onDistanceClick(distance: DistanceEntity)
    }
}
