const params = new URLSearchParams(window.location.search);
const editId = params.get("id");
let selectedDiet = "";

function showAlert(message, type = "error") {
  document.getElementById("alert-box").innerHTML = `<div class="alert alert-${type}">${message}</div>`;
}

function val(id) {
  return document.getElementById(id).value.trim();
}

function numOrNull(id) {
  const v = document.getElementById(id).value;
  return v === "" ? null : Number(v);
}

document.querySelectorAll("#diet-type-group .chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    document.querySelectorAll("#diet-type-group .chip").forEach((c) => c.classList.remove("active"));
    chip.classList.add("active");
    selectedDiet = chip.dataset.diet;
  });
});

async function requireLogin() {
  try {
    return await Api.get("/api/profile/me");
  } catch (e) {
    window.location.href = "login.html";
    return null;
  }
}

async function prefillForEdit() {
  const recipe = await Api.get(`/api/recipes/${editId}`);
  const me = await requireLogin();
  if (!me) return;
  if (me.username !== recipe.creatorUsername) {
    showAlert("You can only edit your own recipes");
    document.getElementById("recipe-form").classList.add("hidden");
    return;
  }

  document.getElementById("form-title").textContent = "Edit Recipe";
  document.getElementById("submit-btn").textContent = "Save Changes";
  // Veg/Non-Veg is immutable after creation (JPA single-table inheritance) -- hide the selector on edit.
  document.getElementById("diet-type-group").classList.add("hidden");

  document.getElementById("recipe-name").value = recipe.recipeName;
  document.getElementById("ingredients").value = recipe.ingredients;
  document.getElementById("method").value = recipe.method;
  document.getElementById("required-equipment").value = recipe.requiredEquipment || "";
  document.getElementById("dietary-tag").value = recipe.dietaryTag || "";
  document.getElementById("cooking-time").value = recipe.cookingTimeMinutes || "";
  document.getElementById("calories").value = recipe.calories || "";
  document.getElementById("protein").value = recipe.protein || "";
  document.getElementById("cost").value = recipe.cost != null ? recipe.cost : "";
  document.getElementById("speed").value = recipe.speedRating || "";
  document.getElementById("difficulty").value = recipe.difficultyRating || "";
  document.getElementById("cuisine-region").value = recipe.cuisineRegion || "";
  document.getElementById("food-type").value = recipe.foodType || "";
}

function buildRecipeJson() {
  const base = {
    recipeName: val("recipe-name"),
    ingredients: val("ingredients"),
    method: val("method"),
    requiredEquipment: val("required-equipment") || null,
    dietaryTag: val("dietary-tag") || null,
    cookingTimeMinutes: numOrNull("cooking-time"),
    calories: numOrNull("calories"),
    protein: numOrNull("protein"),
    cost: numOrNull("cost"),
    speedRating: numOrNull("speed"),
    difficultyRating: numOrNull("difficulty"),
    cuisineRegion: val("cuisine-region") || null,
    foodType: val("food-type") || null,
  };
  if (!editId) {
    base.dietType = selectedDiet;
  }
  return base;
}

function buildFormData() {
  const form = new FormData();
  form.append("recipe", new Blob([JSON.stringify(buildRecipeJson())], { type: "application/json" }));
  const image = document.getElementById("image-file").files[0];
  const video = document.getElementById("video-file").files[0];
  if (image) form.append("image", image);
  if (video) form.append("video", video);
  return form;
}

document.getElementById("recipe-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  document.getElementById("alert-box").innerHTML = "";

  if (!editId && !selectedDiet) {
    showAlert("Please select Veg or Non-Veg");
    return;
  }

  try {
    const formData = buildFormData();
    const recipe = editId
      ? await Api.putForm(`/api/recipes/${editId}`, formData)
      : await Api.postForm("/api/recipes", formData);
    window.location.href = `recipe.html?id=${recipe.id}`;
  } catch (err) {
    showAlert(err.message);
  }
});

(async () => {
  if (editId) {
    await prefillForEdit();
  } else {
    await requireLogin();
  }
})();
