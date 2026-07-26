1|package com.example.plugins.planner.ui.components
2|
3|4|import androidx.compose.animation.AnimatedVisibility
5|import androidx.compose.animation.expandVertically
6|import androidx.compose.animation.shrinkVertically
7|import androidx.compose.foundation.background
8|import androidx.compose.foundation.clickable
9|import androidx.compose.foundation.layout.*
10|import androidx.compose.foundation.shape.RoundedCornerShape
11|import androidx.compose.material.icons.Icons
12|import androidx.compose.material.icons.filled.Check
13|import androidx.compose.material.icons.filled.Close
14|import androidx.compose.material.icons.filled.KeyboardArrowDown
15|import androidx.compose.material.icons.filled.KeyboardArrowUp
16|import androidx.compose.material3.*
17|import androidx.compose.runtime.*
18|import androidx.compose.ui.Alignment
19|import androidx.compose.ui.Modifier
20|import androidx.compose.ui.draw.clip
21|import androidx.compose.ui.layout.ContentScale
22|import androidx.compose.ui.platform.LocalContext
23|import androidx.compose.ui.text.font.FontWeight
24|import androidx.compose.ui.text.style.TextOverflow
25|import androidx.compose.ui.unit.dp
26|import androidx.compose.ui.unit.sp
27|import coil.compose.AsyncImage
28|import coil.request.ImageRequest
29|import com.example.core.util.RTL
30|import com.example.plugins.planner.data.ActivityAttachment
31|import com.example.plugins.planner.data.ActivityMessageModel
32|import com.example.plugins.planner.data.StepCardModel
33|
34|/**
35| * StepCard — Rich step card with activity preview.
36| *
37| * Architecture (Phase 4.9.3 + 4.9.4):
38| * - Collapsed: title + summary
39| * - Expanded: title + activity messages
40| * - RTL compatible
41| * - Neumorphic design system
42| * - Material3 components
43| */
44|@Composable
45|fun StepCard(
46|    model: StepCardModel,
47|    onToggle: () -> Unit,
48|    onDelete: () -> Unit,
49|    onAddActivity: (Int) -> Unit,
50|    modifier: Modifier = Modifier
51|) {
52|    var isExpanded by remember { mutableStateOf(false) }
53|
54|    // Debug: Log StepCard rendering
56|    model.messages.forEachIndexed { idx, msg ->
57|        val imageAttachments = msg.attachments.filterIsInstance<ActivityAttachment.Image>()
58|        if (imageAttachments.isNotEmpty()) {
60|            imageAttachments.forEach { img ->
62|            }
63|        }
64|    }
65|
66|    NeumorphicSurface(
67|        modifier = modifier
68|            .fillMaxWidth()
69|            .clip(RoundedCornerShape(12.dp)),
70|        shape = RoundedCornerShape(12.dp),
71|        elevation = 3
72|    ) {
73|        Column(
74|            modifier = Modifier
75|                .fillMaxWidth()
76|                .padding(12.dp)
77|        ) {
78|            // ── Header: Checkbox + Title + Actions ──
79|            Row(
80|                modifier = Modifier.fillMaxWidth(),
81|                verticalAlignment = Alignment.CenterVertically
82|            ) {
83|                Checkbox(
84|                    checked = model.isCompleted,
85|                    onCheckedChange = { onToggle() },
86|                    modifier = Modifier.size(22.dp),
87|                    colors = CheckboxDefaults.colors(
88|                        checkedColor = MaterialTheme.colorScheme.primary,
89|                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
90|                    )
91|                )
92|
93|                Spacer(modifier = Modifier.width(10.dp))
94|
95|                Column(modifier = Modifier.weight(1f)) {
96|                    Text(
97|                        text = "${RTL}${model.title}",
98|                        fontSize = 13.sp,
99|                        fontWeight = FontWeight.Medium,
100|                        color = if (model.isCompleted)
101|                            MaterialTheme.colorScheme.onSurfaceVariant
102|                        else
103|                            MaterialTheme.colorScheme.onSurface,
104|                        maxLines = 2,
105|                        overflow = TextOverflow.Ellipsis
106|                    )
107|
108|                    // Activity summary
109|                    if (model.hasRichContent()) {
110|                        Spacer(modifier = Modifier.height(4.dp))
111|                        Text(
112|                            text = model.getActivitySummary(),
113|                            fontSize = 11.sp,
114|                            color = MaterialTheme.colorScheme.onSurfaceVariant
115|                        )
116|                    }
117|                }
118|
119|                // Expand/Collapse button
120|                if (model.messages.isNotEmpty()) {
121|                    IconButton(
122|                        onClick = { isExpanded = !isExpanded },
123|                        modifier = Modifier.size(32.dp)
124|                    ) {
125|                        Icon(
126|                            imageVector = if (isExpanded)
127|                                Icons.Default.KeyboardArrowUp
128|                            else
129|                                Icons.Default.KeyboardArrowDown,
130|                            contentDescription = if (isExpanded) "${RTL}بستن" else "${RTL}باز کردن",
131|                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
132|                            modifier = Modifier.size(20.dp)
133|                        )
134|                    }
135|                }
136|
137|                // Delete button
138|                IconButton(
139|                    onClick = onDelete,
140|                    modifier = Modifier.size(32.dp)
141|                ) {
142|                    Icon(
143|                        imageVector = Icons.Default.Close,
144|                        contentDescription = "${RTL}حذف",
145|                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
146|                        modifier = Modifier.size(16.dp)
147|                    )
148|                }
149|            }
150|
151|            // ── Expanded Content: Activity Messages ──
152|            AnimatedVisibility(
153|                visible = isExpanded,
154|                enter = expandVertically(),
155|                exit = shrinkVertically()
156|            ) {
157|                Column(
158|                    modifier = Modifier
159|                        .fillMaxWidth()
160|                        .padding(top = 12.dp),
161|                    verticalArrangement = Arrangement.spacedBy(8.dp)
162|                ) {
163|                    // Activity count header
164|                    Text(
165|                        text = "فعالیت‌ها (${model.messages.size})",
166|                        fontSize = 11.sp,
167|                        fontWeight = FontWeight.Bold,
168|                        color = MaterialTheme.colorScheme.primary,
169|                        modifier = Modifier.padding(bottom = 4.dp)
170|                    )
171|
172|                    // Activity messages
173|                    model.messages.forEach { message ->
174|                        ActivityMessageCard(message = message)
175|                    }
176|
177|                    // Add activity button
178|                    Surface(
179|                        modifier = Modifier
180|                            .fillMaxWidth()
181|                            .clickable { onAddActivity(model.id.toInt()) },
182|                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
183|                        shape = RoundedCornerShape(8.dp)
184|                    ) {
185|                        Row(
186|                            modifier = Modifier
187|                                .fillMaxWidth()
188|                                .padding(10.dp),
189|                            verticalAlignment = Alignment.CenterVertically,
190|                            horizontalArrangement = Arrangement.Center
191|                        ) {
192|                            Text(
193|                                text = "+",
194|                                fontSize = 14.sp,
195|                                fontWeight = FontWeight.Bold,
196|                                color = MaterialTheme.colorScheme.primary
197|                            )
198|                            Spacer(modifier = Modifier.width(4.dp))
199|                            Text(
200|                                text = "افزودن فعالیت",
201|                                fontSize = 12.sp,
202|                                color = MaterialTheme.colorScheme.primary
203|                            )
204|                        }
205|                    }
206|                }
207|            }
208|        }
209|    }
210|}
211|
212|/**
213| * ActivityMessageCard — Shows a single activity message.
214| */
215|@Composable
216|private fun ActivityMessageCard(
217|    message: ActivityMessageModel,
218|    modifier: Modifier = Modifier
219|) {
220|    Surface(
221|        modifier = modifier.fillMaxWidth(),
222|        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
223|        shape = RoundedCornerShape(8.dp)
224|    ) {
225|        Column(
226|            modifier = Modifier
227|                .fillMaxWidth()
228|                .padding(10.dp)
229|        ) {
230|            // Message header with icon
231|            Row(
232|                verticalAlignment = Alignment.CenterVertically,
233|                horizontalArrangement = Arrangement.spacedBy(6.dp)
234|            ) {
235|                Text(
236|                    text = getMessageIcon(message),
237|                    fontSize = 12.sp
238|                )
239|                Text(
240|                    text = getMessageLabel(message),
241|                    fontSize = 11.sp,
242|                    fontWeight = FontWeight.Medium,
243|                    color = MaterialTheme.colorScheme.onSurfaceVariant
244|                )
245|            }
246|
247|            // Text content
248|            message.text?.let { text ->
249|                if (text.isNotBlank()) {
250|                    Spacer(modifier = Modifier.height(6.dp))
251|                    Text(
252|                        text = text,
253|                        fontSize = 12.sp,
254|                        color = MaterialTheme.colorScheme.onSurface,
255|                        maxLines = 4,
256|                        overflow = TextOverflow.Ellipsis
257|                    )
258|                }
259|            }
260|
261|            // Image attachments
262|            val images = message.attachments.filterIsInstance<ActivityAttachment.Image>()
264|            images.forEachIndexed { idx, img ->
266|            }
267|            if (images.isNotEmpty()) {
268|                Spacer(modifier = Modifier.height(8.dp))
269|                images.forEach { image ->
270|                    AsyncImage(
271|                        model = ImageRequest.Builder(LocalContext.current)
272|                            .data(android.net.Uri.parse(image.uri))
273|                            .crossfade(true)
274|                            .build(),
275|                        contentDescription = "${RTL}تصویر پیوست",
276|                        modifier = Modifier
277|                            .fillMaxWidth()
278|                            .height(120.dp)
279|                            .clip(RoundedCornerShape(8.dp)),
280|                        contentScale = ContentScale.Crop
281|                    )
282|                }
283|            }
284|
285|            // Duration
286|            message.durationMinutes?.let { duration ->
287|                Spacer(modifier = Modifier.height(6.dp))
288|                Text(
289|                    text = "⏱️ $duration ${RTL}دقیقه",
290|                    fontSize = 11.sp,
291|                    color = MaterialTheme.colorScheme.primary
292|                )
293|            }
294|        }
295|    }
296|}
297|
298|/**
299| * Get icon for message type.
300| */
301|private fun getMessageIcon(message: ActivityMessageModel): String {
302|    return when {
303|        message.isStep -> "✓"
304|        message.attachments.isNotEmpty() -> "📷"
305|        message.durationMinutes != null -> "📌"
306|        else -> "📝"
307|    }
308|}
309|
310|/**
311| * Get label for message type.
312| */
313|private fun getMessageLabel(message: ActivityMessageModel): String {
314|    return when {
315|        message.isStep -> "مرحله"
316|        message.attachments.isNotEmpty() -> "تصویر"
317|        message.durationMinutes != null -> "فعالیت دستی"
318|        else -> "یادداشت"
319|    }
320|}
321|