if (vm.channels.isNotEmpty()) {
            OutlinedTextField(
                value = vm.query, onValueChange = { vm.query = it },
                label = { Text("بحث") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(vm.selectedGroup == null, { vm.selectedGroup = null }, { Text("الكل") }) }
                items(vm.groups) { g ->
                    FilterChip(vm.selectedGroup == g, { vm.selectedGroup = g }, { Text(g, maxLines = 1) })
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.filtered, key = { it.url }) { c ->
                    Card(onClick = { onPlay(c) }) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = c.logo, contentDescription = c.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(80.dp).fillMaxWidth()
                            )
                            Text(c.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(channel.url)); prepare(); playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player; useController = true; requestFocus() } },
        modifier = Modifier.fillMaxSize()
    )
}
