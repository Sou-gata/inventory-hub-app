package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

public class OnlineCategoryDto {

    @SerializedName(value = "id", alternate = {"category_id", "categoryId"})
    public int id;

    @SerializedName(value = "name", alternate = {"category_name", "categoryName"})
    public String name;

    @SerializedName(value = "description", alternate = {"desc"})
    public String description;

    @SerializedName(value = "isActive", alternate = {"is_active"})
    public boolean isActive = true;

    public OnlineCategoryDto() {
    }

    public OnlineCategoryDto(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
