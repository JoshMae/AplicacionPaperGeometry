package com.example.papergemoetry.models

import android.os.Parcel
import android.os.Parcelable

data class Character(
    val idProducto: Int,
    val nombre: String,
    val precio: String,
    val detalle: String,
    val categoria: String,
    val fecha_registro: String,
    val foto: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(idProducto)
        parcel.writeString(nombre)
        parcel.writeString(precio)
        parcel.writeString(detalle)
        parcel.writeString(categoria)
        parcel.writeString(fecha_registro)
        parcel.writeString(foto)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Character> {
        override fun createFromParcel(parcel: Parcel): Character {
            return Character(parcel)
        }

        override fun newArray(size: Int): Array<Character?> {
            return arrayOfNulls(size)
        }
    }
}



